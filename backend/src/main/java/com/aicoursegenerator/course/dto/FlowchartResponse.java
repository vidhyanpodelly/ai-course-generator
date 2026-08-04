package com.aicoursegenerator.course.dto;

import java.util.List;

public record FlowchartResponse(
    List<FlowchartNode> nodes,
    List<FlowchartEdge> edges
) {
    public record FlowchartNode(
        String id,
        String label
    ) {}

    public record FlowchartEdge(
        String fromId,
        String toId,
        String label
    ) {}
}
