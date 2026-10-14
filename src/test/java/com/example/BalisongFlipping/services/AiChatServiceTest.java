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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;

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

        StreamingResponseBody body = aiChatService.streamChat("session-1", "Hi", "access-token", "/chat", "website");

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

        StreamingResponseBody body = aiChatService.streamChat("session-1", "Hi", "access-token", "/chat", "website");

        assertThrows(IOException.class, () -> body.writeTo(new ByteArrayOutputStream()));
        assertTrue(Thread.interrupted());
    }

    @Test
    void streamChatFlushesEachChunkAsItArrives() throws Exception {
        byte[] payload = "x".repeat(2500).getBytes();
        when(httpResponse.body()).thenReturn(new ByteArrayInputStream(payload));
        org.mockito.Mockito.doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        int[] flushCount = {0};
        ByteArrayOutputStream out = new ByteArrayOutputStream() {
            @Override
            public void flush() {
                flushCount[0]++;
            }
        };

        aiChatService.streamChat("session-1", "Hi", "access-token", "/chat", "website").writeTo(out);

        assertEquals(2500, out.size());
        assertEquals(3, flushCount[0]);
    }

    @Test
    void streamChatForwardsTheCallingClientToTheAiService() throws Exception {
        when(httpResponse.body()).thenReturn(new ByteArrayInputStream(new byte[0]));
        org.mockito.Mockito.doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        aiChatService.streamChat("session-1", "Hi", null, null, "discord").writeTo(new ByteArrayOutputStream());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        org.mockito.Mockito.verify(httpClient).send(requestCaptor.capture(), any());
        assertTrue(bodyOf(requestCaptor.getValue()).contains("\"client\":\"discord\""));
    }

    private static String bodyOf(HttpRequest request) {
        StringBuilder body = new StringBuilder();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer item) { body.append(StandardCharsets.UTF_8.decode(item)); }
            public void onError(Throwable throwable) {}
            public void onComplete() {}
        });
        return body.toString();
    }
}
