package com.aicoursegenerator.course.controller;

import com.aicoursegenerator.common.dto.ApiResponse;
import com.aicoursegenerator.course.entity.Flashcard;
import com.aicoursegenerator.course.service.FlashcardService;
import com.aicoursegenerator.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
public class FlashcardController {

    private final FlashcardService flashcardService;

    public record FlashcardResponse(
            UUID id,
            UUID chapterId,
            String front,
            String back,
            String difficulty,
            int box,
            ZonedDateTime nextReview
    ) {}

    public FlashcardController(FlashcardService flashcardService) {
        this.flashcardService = flashcardService;
    }

    @GetMapping("/chapters/{chapterId}/flashcards")
    public ResponseEntity<ApiResponse<List<FlashcardResponse>>> getOrGenerateFlashcards(
            @PathVariable("chapterId") UUID chapterId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Flashcard> cards = flashcardService.getOrGenerateFlashcards(chapterId, userDetails.getUser());
        List<FlashcardResponse> response = cards.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Flashcards loaded successfully", response));
    }

    @GetMapping("/{courseId}/flashcards")
    public ResponseEntity<ApiResponse<List<FlashcardResponse>>> getCardsForCourse(
            @PathVariable("courseId") UUID courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Flashcard> cards = flashcardService.getCardsForCourse(courseId, userDetails.getUser());
        List<FlashcardResponse> response = cards.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Course flashcards loaded successfully", response));
    }

    @GetMapping("/flashcards/due")
    public ResponseEntity<ApiResponse<List<FlashcardResponse>>> getDueReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Flashcard> cards = flashcardService.getDueReviews(userDetails.getUser());
        List<FlashcardResponse> response = cards.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Due reviews loaded successfully", response));
    }

    @PostMapping("/flashcards/{cardId}/review")
    public ResponseEntity<ApiResponse<FlashcardResponse>> reviewFlashcard(
            @PathVariable("cardId") UUID cardId,
            @RequestParam("rating") String rating,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Flashcard card = flashcardService.reviewFlashcard(cardId, rating, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Review logged successfully", mapToResponse(card)));
    }

    private FlashcardResponse mapToResponse(Flashcard f) {
        return new FlashcardResponse(
                f.getId(),
                f.getChapter().getId(),
                f.getFront(),
                f.getBack(),
                f.getDifficulty(),
                f.getBox(),
                f.getNextReview()
        );
    }
}
