package com.aicoursegenerator.course.repository;

import com.aicoursegenerator.course.entity.MindMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MindMapRepository extends JpaRepository<MindMap, UUID> {
    Optional<MindMap> findByChapterId(UUID chapterId);
}
