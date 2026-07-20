package com.aicoursegenerator.course.repository;

import com.aicoursegenerator.course.entity.Bookmark;
import com.aicoursegenerator.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {
    List<Bookmark> findByUserOrderByCreatedAtDesc(User user);
    Optional<Bookmark> findByUserAndBookmarkTypeAndTargetId(User user, String bookmarkType, UUID targetId);
    void deleteByUserAndBookmarkTypeAndTargetId(User user, String bookmarkType, UUID targetId);
}
