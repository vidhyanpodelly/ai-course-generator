package com.aicoursegenerator.course.dto;

import java.util.List;

public record MindMapResponse(
    String rootTopic,
    List<MindMapNode> children
) {
    public record MindMapNode(
        String name,
        List<MindMapNode> children
    ) {}
}
