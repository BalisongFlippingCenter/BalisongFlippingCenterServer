package com.example.BalisongFlipping.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class AiChatService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.service.base-url}")
    private String aiServiceBaseUrl;

    private record PythonChatRequest(String session_id, String message, String access_token, String current_path) {}

    public StreamingResponseBody streamChat(String sessionId, String message, String accessToken, String currentPath) {
        return outputStream -> {
            String body = objectMapper.writeValueAsString(
                    new PythonChatRequest(sessionId, message, accessToken, currentPath)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiServiceBaseUrl + "/chat/stream"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<InputStream> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while calling AI service", e);
            }

            try (InputStream in = response.body()) {
                in.transferTo(outputStream);
            }
        };
    }
}
