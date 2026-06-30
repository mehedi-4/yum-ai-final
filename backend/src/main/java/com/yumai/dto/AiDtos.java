package com.yumai.dto;

import jakarta.validation.constraints.NotBlank;

/** Request/response records for the AI module (FR-05). */
public final class AiDtos {

    private AiDtos() {
    }

    public record ChatRequest(@NotBlank String message) {
    }

    public record ChatResponse(String reply, boolean fromGemini) {
    }
}
