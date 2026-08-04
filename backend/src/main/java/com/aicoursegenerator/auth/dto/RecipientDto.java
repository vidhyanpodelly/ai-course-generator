package com.aicoursegenerator.auth.dto;

public class RecipientDto {
    private String email;
    private String name;

    public RecipientDto() {
    }

    public RecipientDto(String email, String name) {
        this.email = email;
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
