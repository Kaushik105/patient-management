package com.kaushik.authservice.dto;

public class RegisterResponseDto {
    private String message;

    public RegisterResponseDto(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
