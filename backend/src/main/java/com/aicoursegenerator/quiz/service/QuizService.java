package com.aicoursegenerator.quiz.service;

import com.aicoursegenerator.ai.service.AiProvider;
import com.aicoursegenerator.ai.service.AiProviderFactory;
import com.aicoursegenerator.ai.service.PromptLoader;
import com.aicoursegenerator.common.exception.BadRequestException;
import com.aicoursegenerator.common.exception.ResourceNotFoundException;
import com.aicoursegenerator.course.entity.Chapter;
import com.aicoursegenerator.course.entity.QuizQuestion;
import com.aicoursegenerator.course.repository.ChapterRepository;
import com.aicoursegenerator.pdf.entity.PDFChunk;
import com.aicoursegenerator.pdf.repository.PDFChunkRepository;
import com.aicoursegenerator.quiz.entity.QuizAttempt;
import com.aicoursegenerator.quiz.repository.QuizAttemptRepository;
import com.aicoursegenerator.user.entity.User;
import com.aicoursegenerator.user.repository.UserRepository;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class QuizService {

    private static final Logger logger = LoggerFactory.getLogger(QuizService.class);

    private final ChapterRepository chapterRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final PDFChunkRepository chunkRepository;
    private final PromptLoader promptLoader;
    private final AiProviderFactory providerFactory;
    private final UserRepository userRepository;

    public QuizService(
            ChapterRepository chapterRepository,
            QuizAttemptRepository quizAttemptRepository,
            PDFChunkRepository chunkRepository,
            PromptLoader promptLoader,
            AiProviderFactory providerFactory,
            UserRepository userRepository) {
        this.chapterRepository = chapterRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.chunkRepository = chunkRepository;
        this.promptLoader = promptLoader;
        this.providerFactory = providerFactory;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<QuizQuestion> getOrGenerateChapterQuiz(UUID chapterId, User user) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));

        // Ownership validation
        if (!chapter.getCourse().getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Chapter not found for this user");
        }

        // Return cached quiz if exists
        if (chapter.getQuizData() != null && !chapter.getQuizData().isEmpty()) {
            return chapter.getQuizData();
        }

        logger.info("Generating new quiz for Chapter ID: {}", chapterId);

        try {
            UUID pdfMetadataId = chapter.getCourse().getPdfMetadata().getId();
            
            // Match relevant text segments
            List<PDFChunk> relevantChunks = chunkRepository.searchChunks(pdfMetadataId, chapter.getTitle(), 3);
            if (relevantChunks.isEmpty()) {
                List<PDFChunk> allChunks = chunkRepository.findByPdfMetadataOrderByChunkIndexAsc(chapter.getCourse().getPdfMetadata());
                if (!allChunks.isEmpty()) {
                    relevantChunks = allChunks.subList(0, Math.min(allChunks.size(), 3));
                }
            }

            StringBuilder contextBuilder = new StringBuilder();
            for (PDFChunk chunk : relevantChunks) {
                contextBuilder.append(chunk.getContent()).append("\n");
            }

            String template = promptLoader.loadPrompt("quiz_generation.txt");
            Map<String, String> variables = Map.of(
                    "courseTitle", chapter.getCourse().getTitle(),
                    "chapterTitle", chapter.getTitle(),
                    "chapterSummary", chapter.getSummary() != null ? chapter.getSummary() : "",
                    "chunksContext", contextBuilder.toString()
            );

            String userPrompt = promptLoader.interpolate(template, variables);

            AiProvider provider = providerFactory.getProvider();
            QuizQuestion[] questionsArray = provider.generateStructuredJson(
                    "You are a professional academic examiner. Generate valid JSON arrays containing questions.",
                    userPrompt,
                    QuizQuestion[].class
            );

            List<QuizQuestion> questionsList = Arrays.asList(questionsArray);
            
            // Cache quiz in chapter
            chapter.setQuizData(questionsList);
            chapterRepository.save(chapter);

            return questionsList;

        } catch (Exception e) {
            // BUG 4 FIX: Log root cause details \u2014 never swallow the real exception
            Throwable rootCause = e;
            while (rootCause.getCause() != null) rootCause = rootCause.getCause();
            StackTraceElement[] st = rootCause.getStackTrace();
            String location = (st != null && st.length > 0)
                    ? st[0].getFileName() + ":" + st[0].getLineNumber() : "unknown";
            logger.error(
                "Quiz generation FAILED for Chapter ID: {} | Chapter: '{}' | " +
                "Exception type: {} | Root cause: {} | At: {}",
                chapterId,
                chapter.getTitle(),
                e.getClass().getSimpleName(),
                rootCause.getMessage(),
                location,
                e  // full stack trace attached
            );
            throw new RuntimeException("AI Quiz generation failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public QuizAttempt submitQuizAttempt(UUID chapterId, Map<String, String> answers, User user) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));

        if (!chapter.getCourse().getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Chapter not found for this user");
        }

        List<QuizQuestion> questions = chapter.getQuizData();
        if (questions == null || questions.isEmpty()) {
            throw new BadRequestException("No quiz generated for this chapter yet");
        }

        int score = 0;
        int totalQuestions = questions.size();

        for (int i = 0; i < totalQuestions; i++) {
            QuizQuestion question = questions.get(i);
            String questionIndexStr = String.valueOf(i);
            String userAnswer = answers.get(questionIndexStr);

            if (userAnswer != null && isAnswerCorrect(userAnswer, question.getCorrectAnswer())) {
                score++;
            }
        }

        QuizAttempt attempt = new QuizAttempt(null, user, chapter, score, totalQuestions, answers, null);
        updateStreakAndActivity(user);
        return quizAttemptRepository.save(attempt);
    }

    private void updateStreakAndActivity(User user) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime lastActive = user.getLastActiveAt();

        if (lastActive == null) {
            user.setLearningStreak(1);
        } else {
            long diffDays = java.time.temporal.ChronoUnit.DAYS.between(lastActive.toLocalDate(), now.toLocalDate());
            if (diffDays == 1) {
                user.setLearningStreak(user.getLearningStreak() + 1);
            } else if (diffDays > 1) {
                user.setLearningStreak(1);
            }
        }
        user.setLastActiveAt(now);
        userRepository.save(user);
    }

    private boolean isAnswerCorrect(String userAnswer, String correctAnswer) {
        if (userAnswer == null || correctAnswer == null) {
            return false;
        }
        String cleanUser = userAnswer.trim().toLowerCase();
        String cleanCorrect = correctAnswer.trim().toLowerCase();
        
        // Match exact or contains depending on answer format
        if (cleanUser.equals(cleanCorrect)) {
            return true;
        }
        
        // Check standard MCQ prefix matching like "a" or "a)"
        if (cleanCorrect.startsWith(cleanUser) && cleanUser.length() == 1) {
            return true;
        }

        return cleanUser.contains(cleanCorrect) || cleanCorrect.contains(cleanUser);
    }
}
