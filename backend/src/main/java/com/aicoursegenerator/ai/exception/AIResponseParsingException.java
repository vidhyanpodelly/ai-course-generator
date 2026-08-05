package com.aicoursegenerator.ai.exception;

public class AIResponseParsingException extends RuntimeException {
    private String rawResponse;

    public AIResponseParsingException(String message) {
        super(message);
    }
    
    public AIResponseParsingException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public AIResponseParsingException(String message, Throwable cause, String rawResponse) {
        super(message, cause);
        this.rawResponse = rawResponse;
    }

    public AIResponseParsingException(String message, String rawResponse) {
        super(message);
        this.rawResponse = rawResponse;
    }

    public String getRawResponse() {
        return rawResponse;
    }
}
