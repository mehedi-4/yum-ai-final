package com.yumai.controller;

import com.yumai.dto.AiDtos.ChatRequest;
import com.yumai.dto.AiDtos.ChatResponse;
import com.yumai.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** FR-05 / UC-14 - AI chat assistant (Manager/Admin). */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return aiService.chat(request.message());
    }
}