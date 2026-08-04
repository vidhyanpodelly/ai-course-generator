package com.aicoursegenerator.auth.dto;

public class ResendEmailResponse {
    private String id;

    public ResendEmailResponse() {
    }

    public ResendEmailResponse(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
