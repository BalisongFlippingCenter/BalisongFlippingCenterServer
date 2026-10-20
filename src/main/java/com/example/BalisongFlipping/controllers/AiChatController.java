package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.AiChatRequestDto;
import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.AiChatService;
import com.example.BalisongFlipping.services.AiClientAuthService;
import com.example.BalisongFlipping.services.AiRateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
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

    @Autowired
    private AccountService accountService;

    @PostMapping("/chat")
    public ResponseEntity<StreamingResponseBody> chat(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestHeader("X-Client-Key") String clientKey,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody AiChatRequestDto request
    ) {
        if (!aiClientAuthService.isValid(clientId, clientKey)) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "Invalid client credentials.");
        }

        if (!aiRateLimiterService.tryAcquire(request.sessionId())) {
            return errorResponse(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded.");
        }

        String accessToken = null;
        try {
            accountService.getSelf();
            accessToken = authorization;
        } catch (Exception ignored) {
        }

        StreamingResponseBody body = aiChatService.streamChat(request.sessionId(), request.message(), accessToken, request.currentPath(), clientId);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(body);
    }

    @DeleteMapping("/chat/session/{sessionId}")
    public ResponseEntity<String> clearSession(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestHeader("X-Client-Key") String clientKey,
            @PathVariable String sessionId
    ) throws IOException {
        if (!aiClientAuthService.isValid(clientId, clientKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid client credentials.");
        }

        if (!sessionId.startsWith(clientId + "-")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Clients can only clear their own sessions.");
        }

        if (!aiChatService.clearSession(sessionId)) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("AI service could not clear the session.");
        }

        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<StreamingResponseBody> errorResponse(HttpStatus status, String message) {
        StreamingResponseBody body = out -> out.write(message.getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.status(status).contentType(MediaType.TEXT_PLAIN).body(body);
    }
}
