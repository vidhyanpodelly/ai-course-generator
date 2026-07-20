package com.aicoursegenerator.course.controller;

import com.aicoursegenerator.common.dto.ApiResponse;
import com.aicoursegenerator.common.exception.ResourceNotFoundException;
import com.aicoursegenerator.course.entity.Bookmark;
import com.aicoursegenerator.course.repository.BookmarkRepository;
import com.aicoursegenerator.security.CustomUserDetails;
import com.aicoursegenerator.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookmarks")
@Transactional
public class BookmarkController {

    private final BookmarkRepository bookmarkRepository;

    public record BookmarkRequest(
            String bookmarkType, // LESSON, CHAPTER, CHAT, SEARCH
            UUID targetId,
            String title,
            String content
    ) {}

    public record BookmarkResponse(
            UUID id,
            String bookmarkType,
            UUID targetId,
            String title,
            String content,
            ZonedDateTime createdAt
    ) {}

    public BookmarkController(BookmarkRepository bookmarkRepository) {
        this.bookmarkRepository = bookmarkRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookmarkResponse>>> getBookmarks(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Bookmark> list = bookmarkRepository.findByUserOrderByCreatedAtDesc(userDetails.getUser());
        List<BookmarkResponse> resp = list.stream().map(this::mapToResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Bookmarks retrieved successfully", resp));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookmarkResponse>> addBookmark(
            @RequestBody BookmarkRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        User user = userDetails.getUser();
        
        // Prevent duplicates
        Optional<Bookmark> existing = bookmarkRepository.findByUserAndBookmarkTypeAndTargetId(
                user, request.bookmarkType().toUpperCase(), request.targetId());
        
        if (existing.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success("Already bookmarked", mapToResponse(existing.get())));
        }

        Bookmark bookmark = new Bookmark(
                UUID.randomUUID(),
                user,
                request.bookmarkType().toUpperCase(),
                request.targetId(),
                request.title(),
                request.content()
        );

        Bookmark saved = bookmarkRepository.save(bookmark);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bookmark created successfully", mapToResponse(saved)));
    }

    @DeleteMapping("/{bookmarkId}")
    public ResponseEntity<ApiResponse<Void>> deleteBookmark(
            @PathVariable("bookmarkId") UUID bookmarkId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found"));

        if (!bookmark.getUser().getId().equals(userDetails.getUser().getId())) {
            throw new ResourceNotFoundException("Bookmark not found for this user");
        }

        bookmarkRepository.delete(bookmark);
        return ResponseEntity.ok(ApiResponse.success("Bookmark deleted successfully", null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteBookmarkByTypeAndTarget(
            @RequestParam("type") String type,
            @RequestParam("targetId") UUID targetId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Optional<Bookmark> bookmark = bookmarkRepository.findByUserAndBookmarkTypeAndTargetId(
                userDetails.getUser(), type.toUpperCase(), targetId);
        
        if (bookmark.isPresent()) {
            bookmarkRepository.delete(bookmark.get());
        }
        
        return ResponseEntity.ok(ApiResponse.success("Bookmark removed successfully", null));
    }

    private BookmarkResponse mapToResponse(Bookmark b) {
        return new BookmarkResponse(
                b.getId(),
                b.getBookmarkType(),
                b.getTargetId(),
                b.getTitle(),
                b.getContent(),
                b.getCreatedAt()
        );
    }
}
