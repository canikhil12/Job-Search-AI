package com.jobmatch.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatch.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

/**
 * Streams the résumé↔job gap analysis to the browser as Server-Sent Events. Ownership + existence
 * are validated synchronously (so a missing résumé/job returns a normal 404 JSON before the stream
 * starts); the model's tokens are then relayed on a background thread. Each token is sent JSON-encoded
 * so newlines in the text can't break SSE framing.
 */
@RestController
public class AnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AnalysisController.class);
    private static final long TIMEOUT_MS = 180_000L;

    private final AnalysisService analysisService;
    private final TaskExecutor taskExecutor;
    private final ObjectMapper objectMapper;

    public AnalysisController(AnalysisService analysisService,
                             TaskExecutor taskExecutor,
                             ObjectMapper objectMapper) {
        this.analysisService = analysisService;
        this.taskExecutor = taskExecutor;
        this.objectMapper = objectMapper;
    }

    // No `produces` here on purpose: SseEmitter sets text/event-stream itself, and leaving the
    // producible type unpinned lets a pre-stream error (e.g. 404) render as a normal JSON ApiError.
    @GetMapping("/api/resumes/{resumeId}/jobs/{jobId}/analysis")
    public SseEmitter analyze(@AuthenticationPrincipal User user,
                              @PathVariable UUID resumeId,
                              @PathVariable UUID jobId) {
        // Synchronous: any 404 surfaces as a normal JSON error before streaming begins.
        AnalysisService.Prompt prompt = analysisService.prepare(user.getId(), resumeId, jobId);

        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        taskExecutor.execute(() -> {
            try {
                analysisService.stream(prompt, token -> send(emitter, token));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception ex) {
                log.warn("Gap analysis stream failed: {}", ex.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error").data("Analysis failed"));
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
