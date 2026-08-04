package com.aicoursegenerator.auth.dto;

import java.util.List;

public class ResendEmailRequest {
    private String from;
    private List<String> to;
    private String subject;
    private String html;

    public ResendEmailRequest() {
    }

    public ResendEmailRequest(String from, List<String> to, String subject, String html) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.html = html;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }
}
