package com.example.BalisongFlipping.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class AiChatService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.service.base-url}")
    private String aiServiceBaseUrl;

    @Value("${ai.service.shared-secret}")
    private String aiServiceSharedSecret;

    private record PythonChatRequest(String session_id, String message, String access_token, String current_path, String client) {}

    public boolean clearSession(String sessionId) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiServiceBaseUrl + "/chat/session/" + URLEncoder.encode(sessionId, StandardCharsets.UTF_8)))
                .header("X-Internal-Secret", aiServiceSharedSecret)
                .timeout(Duration.ofSeconds(10))
                .DELETE()
                .build();

        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() / 100 == 2;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while calling AI service", e);
        }
    }

    public StreamingResponseBody streamChat(String sessionId, String message, String accessToken, String currentPath, String clientId) {
        return outputStream -> {
            String body = objectMapper.writeValueAsString(
                    new PythonChatRequest(sessionId, message, accessToken, currentPath, clientId)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiServiceBaseUrl + "/chat/stream"))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Secret", aiServiceSharedSecret)
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
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }
            }
        };
    }
}
