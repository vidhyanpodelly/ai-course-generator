package com.aicoursegenerator.chat.repository;

import com.aicoursegenerator.chat.entity.ChatMessage;
import com.aicoursegenerator.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session);
    void deleteBySession(ChatSession session);
}
