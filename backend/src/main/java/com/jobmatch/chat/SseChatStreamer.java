package com.jobmatch.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * Streams a chat completion to the browser as Server-Sent Events. Shared by the gap-analysis and
 * cover-letter endpoints: builds an {@link SseEmitter}, relays the model's tokens on a background
 * thread (each JSON-encoded so newlines can't break SSE framing), and emits a final {@code done}
 * (or {@code error}) event.
 */
@Component
public class SseChatStreamer {

    private static final Logger log = LoggerFactory.getLogger(SseChatStreamer.class);
    private static final long TIMEOUT_MS = 180_000L;

    private final ChatClient chatClient;
    private final TaskExecutor taskExecutor;
    private final ObjectMapper objectMapper;

    public SseChatStreamer(ChatClient chatClient, TaskExecutor taskExecutor, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.taskExecutor = taskExecutor;
        this.objectMapper = objectMapper;
    }

    public SseEmitter stream(String system, String user) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        taskExecutor.execute(() -> {
            try {
                chatClient.streamCompletion(system, user, token -> send(emitter, token));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception ex) {
                log.warn("Chat stream failed: {}", ex.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error").data("Generation failed"));
                } catch (IOException ignored) {
                    // client already gone
                }
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    private void send(SseEmitter emitter, String token) {
        try {
            // JSON-encode so text containing newlines stays a single SSE data line.
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(token)));
        } catch (IOException ex) {
            throw new IllegalStateException("SSE send failed", ex);
        }
    }
}
