package com.aicoursegenerator.course.service;

import com.aicoursegenerator.course.dto.FlowchartResponse;
import com.aicoursegenerator.course.dto.MindMapResponse;
import com.aicoursegenerator.course.dto.SequenceResponse;
import org.springframework.stereotype.Service;

@Service
public class MermaidGenerator {

    public String generateMindMap(MindMapResponse response) {
        if (response == null || response.rootTopic() == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("mindmap\n");
        sb.append("  root((\"").append(sanitizeLabel(response.rootTopic())).append("\"))\n");
        if (response.children() != null) {
            for (MindMapResponse.MindMapNode child : response.children()) {
                appendNode(sb, child, 4);
            }
        }
        return sb.toString();
    }

    private void appendNode(StringBuilder sb, MindMapResponse.MindMapNode node, int indentLevel) {
        if (node == null || node.name() == null || node.name().isEmpty()) return;
        String indent = " ".repeat(indentLevel);
        String safeId = "node_" + Math.abs(node.name().hashCode()) + "_" + (int)(Math.random() * 10000);
        sb.append(indent).append(safeId).append("[\"").append(sanitizeLabel(node.name())).append("\"]\n");
        if (node.children() != null) {
            for (MindMapResponse.MindMapNode child : node.children()) {
                appendNode(sb, child, indentLevel + 2);
            }
        }
    }

    public String generateFlowchart(FlowchartResponse response) {
        if (response == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("graph TD\n");
        if (response.nodes() != null) {
            for (FlowchartResponse.FlowchartNode node : response.nodes()) {
                sb.append("  ").append(sanitizeId(node.id())).append("[\"").append(sanitizeLabel(node.label())).append("\"]\n");
            }
        }
        if (response.edges() != null) {
            for (FlowchartResponse.FlowchartEdge edge : response.edges()) {
                sb.append("  ").append(sanitizeId(edge.fromId())).append(" --> ");
                if (edge.label() != null && !edge.label().isEmpty()) {
                    sb.append("|\"").append(sanitizeLabel(edge.label())).append("\"| ");
                }
                sb.append(sanitizeId(edge.toId())).append("\n");
            }
        }
        return sb.toString();
    }

    public String generateSequence(SequenceResponse response) {
        if (response == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("sequenceDiagram\n");
        if (response.actors() != null) {
            for (String actor : response.actors()) {
                sb.append("  participant ").append(sanitizeId(actor)).append("\n");
            }
        }
        if (response.messages() != null) {
            for (SequenceResponse.SequenceMessage msg : response.messages()) {
                sb.append("  ").append(sanitizeId(msg.from())).append("->>").append(sanitizeId(msg.to())).append(": ").append(sanitizeLabel(msg.message())).append("\n");
            }
        }
        return sb.toString();
    }

    private String sanitize(String text) {
        if (text == null) return "";
        return text.replace("(", "").replace(")", "").replace(":", "").replace("\"", "").replace(";", "").replace("\n", " ").replace("\r", "");
    }
    
    private String sanitizeLabel(String text) {
        if (text == null) return "";
        return text.replace("\"", "'").replace("\n", "<br/>").replace("\r", ""); 
    }

    private String sanitizeId(String id) {
        if (id == null) return "Unknown";
        return id.replaceAll("[^a-zA-Z0-9_]", "");
    }
}
