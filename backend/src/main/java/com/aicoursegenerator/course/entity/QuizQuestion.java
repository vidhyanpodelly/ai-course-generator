package com.aicoursegenerator.course.entity;

import java.util.List;

public class QuizQuestion {
    private String questionText;
    private String type; // MCQ, TRUE_FALSE, SHORT_ANSWER
    private List<String> options; // for MCQ
    private String correctAnswer;
    private String explanation;

    public QuizQuestion() {
    }

    public QuizQuestion(String questionText, String type, List<String> options, String correctAnswer, String explanation) {
        this.questionText = questionText;
        this.type = type;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
    }

    // Getters and Setters
    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
