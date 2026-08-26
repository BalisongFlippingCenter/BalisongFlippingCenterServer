package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.AiChatRequestDto;
import com.example.BalisongFlipping.services.AiChatService;
import com.example.BalisongFlipping.services.AiClientAuthService;
import com.example.BalisongFlipping.services.AiRateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;

@RequestMapping("/ai")
@RestController
public class AiChatController {

    @Autowired
    private AiClientAuthService aiClientAuthService;

    @Autowired
    private AiRateLimiterService aiRateLimiterService;

    @Autowired
    private AiChatService aiChatService;

    @PostMapping("/chat")
    public ResponseEntity<StreamingResponseBody> chat(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestHeader("X-Client-Key") String clientKey,
            @RequestBody AiChatRequestDto request
    ) {
        if (!aiClientAuthService.isValid(clientId, clientKey)) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "Invalid client credentials.");
        }

        if (!aiRateLimiterService.tryAcquire(request.sessionId())) {
            return errorResponse(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded.");
        }

        StreamingResponseBody body = aiChatService.streamChat(request.sessionId(), request.message());
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(body);
    }

    private ResponseEntity<StreamingResponseBody> errorResponse(HttpStatus status, String message) {
        StreamingResponseBody body = out -> out.write(message.getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.status(status).contentType(MediaType.TEXT_PLAIN).body(body);
    }
}
