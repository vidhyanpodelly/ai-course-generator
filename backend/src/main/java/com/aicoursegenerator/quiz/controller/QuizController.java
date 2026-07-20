package com.aicoursegenerator.quiz.controller;

import com.aicoursegenerator.common.dto.ApiResponse;
import com.aicoursegenerator.course.entity.QuizQuestion;
import com.aicoursegenerator.quiz.dto.QuizAttemptResponse;
import com.aicoursegenerator.quiz.dto.QuizSubmissionRequest;
import com.aicoursegenerator.quiz.entity.QuizAttempt;
import com.aicoursegenerator.quiz.service.QuizService;
import com.aicoursegenerator.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/{chapterId}")
    public ResponseEntity<ApiResponse<List<QuizQuestion>>> getQuizQuestions(
            @PathVariable("chapterId") UUID chapterId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<QuizQuestion> questions = quizService.getOrGenerateChapterQuiz(chapterId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved chapter quiz", questions));
    }

    @PostMapping("/{chapterId}/submit")
    public ResponseEntity<ApiResponse<QuizAttemptResponse>> submitQuiz(
            @PathVariable("chapterId") UUID chapterId,
            @RequestBody QuizSubmissionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        QuizAttempt attempt = quizService.submitQuizAttempt(chapterId, request.answers(), userDetails.getUser());
        QuizAttemptResponse response = new QuizAttemptResponse(
                attempt.getId(),
                attempt.getChapter().getId(),
                attempt.getScore(),
                attempt.getTotalQuestions(),
                attempt.getAnswers(),
                attempt.getCreatedAt()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quiz submitted and graded successfully", response));
    }
}
