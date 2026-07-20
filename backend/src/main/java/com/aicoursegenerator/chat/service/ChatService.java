package com.aicoursegenerator.chat.service;

import com.aicoursegenerator.ai.service.AiProviderFactory;
import com.aicoursegenerator.ai.service.AiProvider;
import com.aicoursegenerator.ai.service.PromptLoader;
import com.aicoursegenerator.ai.service.VectorEmbeddingService;
import com.aicoursegenerator.chat.entity.ChatMessage;
import com.aicoursegenerator.chat.entity.ChatSession;
import com.aicoursegenerator.chat.repository.ChatMessageRepository;
import com.aicoursegenerator.chat.repository.ChatSessionRepository;
import com.aicoursegenerator.common.exception.ResourceNotFoundException;
import com.aicoursegenerator.course.entity.Course;
import com.aicoursegenerator.course.repository.CourseRepository;
import com.aicoursegenerator.pdf.entity.PDFChunk;
import com.aicoursegenerator.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final CourseRepository courseRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PromptLoader promptLoader;
    private final AiProviderFactory providerFactory;
    private final VectorEmbeddingService vectorEmbeddingService;

    public ChatService(
            CourseRepository courseRepository,
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            PromptLoader promptLoader,
            AiProviderFactory providerFactory,
            VectorEmbeddingService vectorEmbeddingService) {
        this.courseRepository = courseRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.promptLoader = promptLoader;
        this.providerFactory = providerFactory;
        this.vectorEmbeddingService = vectorEmbeddingService;
    }

    @Transactional
    public List<ChatSession> getChatSessions(UUID courseId, User user) {
        Course course = courseRepository.findByIdAndUser(courseId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        return chatSessionRepository.findByUserAndCourseOrderByUpdatedAtDesc(user, course);
    }

    @Transactional
    public ChatSession createChatSession(UUID courseId, String title, User user) {
        Course course = courseRepository.findByIdAndUser(courseId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        ChatSession session = new ChatSession(null, user, course, title, null, null);
        return chatSessionRepository.save(session);
    }

    @Transactional
    public ChatSession renameChatSession(UUID sessionId, String title, User user) {
        ChatSession session = chatSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Chat Session not found"));
        session.setTitle(title);
        return chatSessionRepository.save(session);
    }

    @Transactional
    public void deleteChatSession(UUID sessionId, User user) {
        ChatSession session = chatSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Chat Session not found"));
        chatMessageRepository.deleteBySession(session);
        chatSessionRepository.delete(session);
    }

    @Transactional
    public void deleteAllChatSessions(UUID courseId, User user) {
        Course course = courseRepository.findByIdAndUser(courseId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        List<ChatSession> sessions = chatSessionRepository.findByUserAndCourseOrderByUpdatedAtDesc(user, course);
        for (ChatSession session : sessions) {
            chatMessageRepository.deleteBySession(session);
            chatSessionRepository.delete(session);
        }
    }

    public List<ChatMessage> getChatHistory(UUID sessionId, User user) {
        ChatSession session = chatSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Chat Session not found"));
        return chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);
    }

    // NOTE: NOT @Transactional — the actual DB save happens inside the onComplete callback
    // on a background thread. Having @Transactional here would close the EntityManager before
    // the thread's onComplete lambda can call chatMessageRepository.save().
    public SseEmitter sendMessageStream(UUID sessionId, String messageContent, User user) {
        logger.info("[STREAM] Received user message for Session {}: '{}'", sessionId, messageContent);
        ChatSession session = chatSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Chat Session not found"));

        UUID courseId = session.getCourse().getId();
        logger.info("Stage: Request received | Course ID: {} | Session ID: {} | User Message: {}", courseId, sessionId, messageContent);

        // 1. Save User Message
        ChatMessage userMessage = new ChatMessage(null, session, "USER", messageContent, null);
        chatMessageRepository.save(userMessage);
        
        // Update session timestamp
        session.setUpdatedAt(java.time.ZonedDateTime.now());
        chatSessionRepository.save(session);

        logger.info("Processing chat question (stream) for Course ID: {}", courseId);

        SseEmitter emitter = new SseEmitter(90000L); // 90 second timeout

        try {
            UUID pdfMetadataId = session.getCourse().getPdfMetadata().getId();

            // 2. Perform Semantic search to find relevant chunks in the PDF
            List<PDFChunk> relevantChunks = vectorEmbeddingService.searchSemantic(pdfMetadataId, messageContent, 4);

            StringBuilder contextBuilder = new StringBuilder();
            for (PDFChunk chunk : relevantChunks) {
                contextBuilder.append("[Chunk Index: ").append(chunk.getChunkIndex()).append("] ")
                              .append(chunk.getContent()).append("\n");
            }
            
            logger.info("Stage: Retrieved chunks | Course ID: {} | Retrieved {} chunks | Chunks Context Length: {}", courseId, relevantChunks.size(), contextBuilder.length());

            // 3. Load Recent Chat History (limit to last 8 messages)
            List<ChatMessage> history = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);
            int startIdx = Math.max(0, history.size() - 8);
            List<ChatMessage> subHistory = history.subList(startIdx, history.size());

            StringBuilder historyBuilder = new StringBuilder();
            for (ChatMessage msg : subHistory) {
                historyBuilder.append(msg.getSender()).append(": ").append(msg.getMessageContent()).append("\n");
            }

            // 4. Load & Interpolate Prompt
            String template = promptLoader.loadPrompt("chat_companion.txt");
            Map<String, String> variables = Map.of(
                    "chunksContext", contextBuilder.toString(),
                    "chatHistory", historyBuilder.toString(),
                    "userQuestion", messageContent
            );

            String userPrompt = promptLoader.interpolate(template, variables);

            // 5. Stream from AI
            String systemPrompt = "You are a helpful and experienced classroom tutor. Rely on the provided context.\n" +
                                  "You must ground every answer in the uploaded documents to prevent hallucinations.\n" +
                                  "Format code blocks, tables, math equations using standard markdown.\n" +
                                  "Always return references to the source chunks/chapters at the end.";

            logger.info("Stage: Prompt sent to AI Provider | Course ID: {} | Session ID: {} | System Prompt Length: {} | User Prompt Length: {}", courseId, sessionId, systemPrompt.length(), userPrompt.length());

            AiProvider provider = providerFactory.getProvider();
            
            provider.streamText(systemPrompt, userPrompt, emitter, (aiAnswerText) -> {
                // Save AI Response to database on complete
                logger.info("Stage: AI response | Course ID: {} | Session ID: {} | Response Length: {}", courseId, sessionId, aiAnswerText.length());
                
                ChatMessage aiMessage = new ChatMessage(null, session, "AI", aiAnswerText, null);
                chatMessageRepository.save(aiMessage);
                
                logger.info("Stage: Database save | Course ID: {} | Session ID: {} | AI message saved successfully", courseId, sessionId);
                logger.info("Stage: Response returned | Course ID: {} | Session ID: {} | Stream completed successfully", courseId, sessionId);
            });

        } catch (Exception e) {
            logger.error("AI chatbot streaming failed for course ID: {} | Error: {}", courseId, e.getMessage(), e);
            try {
                emitter.send(SseEmitter.event().data("Error: Failed to fetch response: " + e.getMessage()));
            } catch (Exception ex) {}
            emitter.completeWithError(e);
        }

        return emitter;
    }
}
