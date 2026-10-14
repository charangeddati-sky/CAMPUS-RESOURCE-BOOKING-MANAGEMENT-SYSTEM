package com.college.booking.controller;

import com.college.booking.dto.AiDtos.BookByLanguageRequest;
import com.college.booking.dto.AiDtos.BookByLanguageResponse;
import com.college.booking.dto.AiDtos.ChatRequest;
import com.college.booking.dto.AiDtos.ChatResponse;
import com.college.booking.dto.AiDtos.InsightsRequest;
import com.college.booking.dto.AiDtos.InterpretRequest;
import com.college.booking.dto.AiDtos.InterpretResponse;
import com.college.booking.security.SecurityUtils;
import com.college.booking.service.AiService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/interpret")
    public InterpretResponse interpret(@Valid @RequestBody InterpretRequest request) {
        return aiService.interpret(request.prompt(), SecurityUtils.currentUser());
    }

    @PostMapping("/recommend")
    public InterpretResponse recommend(@Valid @RequestBody InterpretRequest request) {
        return aiService.interpret(request.prompt(), SecurityUtils.currentUser());
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return aiService.chat(request.message(), request.history(), SecurityUtils.currentUser());
    }

    @PostMapping("/insights")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> insights(@Valid @RequestBody InsightsRequest request) {
        return Map.of("reply", aiService.insights(request.question()));
    }

    @PostMapping("/book")
    public BookByLanguageResponse book(@Valid @RequestBody BookByLanguageRequest request) {
        return aiService.bookFromLanguage(request.prompt(), SecurityUtils.currentUser(), request.confirm());
    }
}
