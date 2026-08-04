package com.aicoursegenerator.auth.dto;

import java.util.List;

public class BrevoEmailRequest {

    private SenderDto sender;
    private List<RecipientDto> to;
    private String subject;
    private String htmlContent;

    public BrevoEmailRequest() {
    }

    public BrevoEmailRequest(SenderDto sender, List<RecipientDto> to, String subject, String htmlContent) {
        this.sender = sender;
        this.to = to;
        this.subject = subject;
        this.htmlContent = htmlContent;
    }

    public SenderDto getSender() {
        return sender;
    }

    public void setSender(SenderDto sender) {
        this.sender = sender;
    }

    public List<RecipientDto> getTo() {
        return to;
    }

    public void setTo(List<RecipientDto> to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }
}
