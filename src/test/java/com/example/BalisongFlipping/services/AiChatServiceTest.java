package com.example.BalisongFlipping.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock private HttpClient httpClient;
    @Mock private HttpResponse<java.io.InputStream> httpResponse;

    private AiChatService aiChatService;

    @BeforeEach
    void setUp() {
        aiChatService = new AiChatService();
        ReflectionTestUtils.setField(aiChatService, "httpClient", httpClient);
        ReflectionTestUtils.setField(aiChatService, "aiServiceBaseUrl", "http://ai-service:9000");
        ReflectionTestUtils.setField(aiChatService, "aiServiceSharedSecret", "shared-secret");
    }

    @Test
    void streamChatSendsExpectedRequestAndStreamsResponse() throws Exception {
        when(httpResponse.body()).thenReturn(new ByteArrayInputStream("hello".getBytes()));
        org.mockito.Mockito.doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        StreamingResponseBody body = aiChatService.streamChat("session-1", "Hi", "access-token", "/chat");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        body.writeTo(out);

        assertEquals("hello", out.toString());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        org.mockito.Mockito.verify(httpClient).send(requestCaptor.capture(), any());
        HttpRequest sentRequest = requestCaptor.getValue();
        assertEquals("http://ai-service:9000/chat/stream", sentRequest.uri().toString());
        assertEquals("shared-secret", sentRequest.headers().firstValue("X-Internal-Secret").orElse(null));
        assertEquals("application/json", sentRequest.headers().firstValue("Content-Type").orElse(null));
    }

    @Test
    void streamChatWrapsInterruptedExceptionAsIOException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any())).thenThrow(new InterruptedException());

        StreamingResponseBody body = aiChatService.streamChat("session-1", "Hi", "access-token", "/chat");

        assertThrows(IOException.class, () -> body.writeTo(new ByteArrayOutputStream()));
        assertTrue(Thread.interrupted());
    }
}
