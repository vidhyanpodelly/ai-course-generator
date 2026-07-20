package com.aicoursegenerator.course.service;

import com.aicoursegenerator.ai.service.AiProvider;
import com.aicoursegenerator.ai.service.AiProviderFactory;
import com.aicoursegenerator.common.exception.ResourceNotFoundException;
import com.aicoursegenerator.course.entity.Chapter;
import com.aicoursegenerator.course.entity.Flashcard;
import com.aicoursegenerator.course.repository.ChapterRepository;
import com.aicoursegenerator.course.repository.FlashcardRepository;
import com.aicoursegenerator.pdf.entity.PDFChunk;
import com.aicoursegenerator.pdf.repository.PDFChunkRepository;
import com.aicoursegenerator.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.*;

@Service
public class FlashcardService {

    private static final Logger logger = LoggerFactory.getLogger(FlashcardService.class);

    private final FlashcardRepository flashcardRepository;
    private final ChapterRepository chapterRepository;
    private final PDFChunkRepository chunkRepository;
    private final AiProviderFactory providerFactory;

    public record FlashcardData(String front, String back) {}

    public FlashcardService(
            FlashcardRepository flashcardRepository,
            ChapterRepository chapterRepository,
            PDFChunkRepository chunkRepository,
            AiProviderFactory providerFactory) {
        this.flashcardRepository = flashcardRepository;
        this.chapterRepository = chapterRepository;
        this.chunkRepository = chunkRepository;
        this.providerFactory = providerFactory;
    }

    @Transactional
    public List<Flashcard> getOrGenerateFlashcards(UUID chapterId, User user) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));

        if (!chapter.getCourse().getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Chapter not found for this user");
        }

        List<Flashcard> existing = flashcardRepository.findByChapterId(chapterId);
        if (!existing.isEmpty()) {
            return existing;
        }

        logger.info("Generating AI Flashcards for Chapter ID: {}", chapterId);

        try {
            UUID pdfMetadataId = chapter.getCourse().getPdfMetadata().getId();
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

            String userPrompt = "Generate 6 educational flashcards for a student studying the chapter: '" + chapter.getTitle() + "'.\n" +
                               "Chapter Summary: " + (chapter.getSummary() != null ? chapter.getSummary() : "") + "\n\n" +
                               "Document context:\n" + contextBuilder.toString() + "\n\n" +
                               "Create question-answer pairs or term-definition pairs.\n" +
                               "You must return a valid JSON array of objects. Each object must have 'front' and 'back' properties.\n" +
                               "Ensure all JSON strings are valid and properly escaped. Output ONLY raw JSON array.";

            AiProvider provider = providerFactory.getProvider();
            
            FlashcardData[] cardsArray = provider.generateStructuredJson(
                    "You are an academic study assistant. Output structured JSON arrays of flashcards.",
                    userPrompt,
                    FlashcardData[].class
            );

            List<Flashcard> savedCards = new ArrayList<>();
            ZonedDateTime now = ZonedDateTime.now();
            
            for (FlashcardData cardData : cardsArray) {
                Flashcard card = new Flashcard(
                        UUID.randomUUID(),
                        chapter,
                        cardData.front(),
                        cardData.back(),
                        "MEDIUM",
                        1,
                        now
                );
                savedCards.add(flashcardRepository.save(card));
            }

            return savedCards;

        } catch (Exception e) {
            logger.error("Failed to generate flashcards for chapter: {}", chapterId, e);
            // Return fallback mock cards
            List<Flashcard> fallbackList = new ArrayList<>();
            FlashcardData[] fallbackData = generateMockCards(chapter.getTitle());
            ZonedDateTime now = ZonedDateTime.now();
            for (FlashcardData fd : fallbackData) {
                Flashcard fc = new Flashcard(UUID.randomUUID(), chapter, fd.front(), fd.back(), "MEDIUM", 1, now);
                fallbackList.add(flashcardRepository.save(fc));
            }
            return fallbackList;
        }
    }

    private FlashcardData[] generateMockCards(String chapterTitle) {
        return new FlashcardData[] {
            new FlashcardData("What is the primary theme of " + chapterTitle + "?", "It explores the core foundations, dynamic variables, and methodology of the topic."),
            new FlashcardData("Why is key validation important in this context?", "It prevents corruption of variables and ensures empirical verification remains robust."),
            new FlashcardData("Explain the significance of control models.", "They establish baseline behaviors to isolate changes caused by targeted parameters."),
            new FlashcardData("What is a common error in study replication?", "Failing to document standard environmental criteria or sky conditions during data gathering."),
            new FlashcardData("How does spaced repetition benefit retention?", "It dynamically reviews elements at expanding intervals to embed them in long-term memory."),
            new FlashcardData("What is the Leitner system?", "A popular method of spaced repetition where flashcards are sorted into boxes based on review accuracy.")
        };
    }

    @Transactional
    public Flashcard reviewFlashcard(UUID cardId, String rating, User user) {
        Flashcard card = flashcardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard not found"));

        if (!card.getChapter().getCourse().getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Flashcard not found for this user");
        }

        ZonedDateTime now = ZonedDateTime.now();
        int box = card.getBox();
        ZonedDateTime nextReview;

        if ("EASY".equalsIgnoreCase(rating)) {
            box = Math.min(5, box + 1);
            // Leitner box intervals for EASY:
            // Box 1: 1 day, Box 2: 3 days, Box 3: 7 days, Box 4: 14 days, Box 5: 30 days
            switch (box) {
                case 1: nextReview = now.plusDays(1); break;
                case 2: nextReview = now.plusDays(3); break;
                case 3: nextReview = now.plusDays(7); break;
                case 4: nextReview = now.plusDays(14); break;
                default: nextReview = now.plusDays(30); break;
            }
        } else if ("MEDIUM".equalsIgnoreCase(rating)) {
            // Keep same box
            // Intervals: Box 1: 12 hours, Box 2: 1 day, Box 3: 3 days, Box 4: 7 days, Box 5: 14 days
            switch (box) {
                case 1: nextReview = now.plusHours(12); break;
                case 2: nextReview = now.plusDays(1); break;
                case 3: nextReview = now.plusDays(3); break;
                case 4: nextReview = now.plusDays(7); break;
                default: nextReview = now.plusDays(14); break;
            }
        } else {
            // HARD: reset box back to 1
            box = 1;
            nextReview = now.plusHours(2); // Review very soon
        }

        card.setBox(box);
        card.setDifficulty(rating.toUpperCase());
        card.setNextReview(nextReview);

        return flashcardRepository.save(card);
    }

    public List<Flashcard> getDueReviews(User user) {
        return flashcardRepository.findDueReviewsForUser(user.getId(), ZonedDateTime.now());
    }

    public List<Flashcard> getCardsForCourse(UUID courseId, User user) {
        return flashcardRepository.findByChapterCourseId(courseId).stream()
                .filter(f -> f.getChapter().getCourse().getUser().getId().equals(user.getId()))
                .toList();
    }

}
