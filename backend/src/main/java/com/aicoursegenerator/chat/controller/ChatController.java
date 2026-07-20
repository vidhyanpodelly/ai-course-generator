package com.aicoursegenerator.chat.controller;

import com.aicoursegenerator.chat.dto.ChatMessageRequest;
import com.aicoursegenerator.chat.dto.ChatMessageResponse;
import com.aicoursegenerator.chat.dto.ChatSessionRequest;
import com.aicoursegenerator.chat.dto.ChatSessionResponse;
import com.aicoursegenerator.chat.entity.ChatMessage;
import com.aicoursegenerator.chat.entity.ChatSession;
import com.aicoursegenerator.chat.service.ChatService;
import com.aicoursegenerator.common.dto.ApiResponse;
import com.aicoursegenerator.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/sessions/{courseId}")
    public ResponseEntity<ApiResponse<List<ChatSessionResponse>>> getChatSessions(
            @PathVariable("courseId") UUID courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ChatSession> sessions = chatService.getChatSessions(courseId, userDetails.getUser());
        List<ChatSessionResponse> dtos = sessions.stream()
                .map(s -> new ChatSessionResponse(s.getId(), s.getCourse().getId(), s.getTitle(), s.getCreatedAt(), s.getUpdatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved chat sessions", dtos));
    }

    @PostMapping("/sessions/{courseId}")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> createChatSession(
            @PathVariable("courseId") UUID courseId,
            @Valid @RequestBody ChatSessionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ChatSession session = chatService.createChatSession(courseId, request.title(), userDetails.getUser());
        ChatSessionResponse dto = new ChatSessionResponse(session.getId(), session.getCourse().getId(), session.getTitle(), session.getCreatedAt(), session.getUpdatedAt());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Successfully created chat session", dto));
    }

    @PutMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> renameChatSession(
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody ChatSessionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ChatSession session = chatService.renameChatSession(sessionId, request.title(), userDetails.getUser());
        ChatSessionResponse dto = new ChatSessionResponse(session.getId(), session.getCourse().getId(), session.getTitle(), session.getCreatedAt(), session.getUpdatedAt());
        return ResponseEntity.ok(ApiResponse.success("Successfully renamed chat session", dto));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteChatSession(
            @PathVariable("sessionId") UUID sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        chatService.deleteChatSession(sessionId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Successfully deleted chat session", null));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getChatHistory(
            @PathVariable("sessionId") UUID sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ChatMessage> history = chatService.getChatHistory(sessionId, userDetails.getUser());
        List<ChatMessageResponse> dtos = history.stream()
                .map(msg -> new ChatMessageResponse(
                        msg.getId(),
                        msg.getSession().getId(),
                        msg.getSender(),
                        msg.getMessageContent(),
                        msg.getCreatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved chat history", dtos));
    }

    @PostMapping(value = "/sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return chatService.sendMessageStream(sessionId, request.message(), userDetails.getUser());
    }
}
