package com.jobmatch.analysis;

import com.jobmatch.chat.SseChatStreamer;
import com.jobmatch.user.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * Streams the résumé↔job gap analysis as Server-Sent Events. Ownership + existence are validated
 * synchronously (so a missing résumé/job returns a normal 404 JSON before the stream starts); the
 * model's tokens are then relayed by {@link SseChatStreamer}. No {@code produces} on purpose:
 * SseEmitter sets text/event-stream itself, and leaving the type unpinned lets a pre-stream error
 * render as JSON.
 */
@RestController
public class AnalysisController {

    private final AnalysisService analysisService;
    private final SseChatStreamer sseChatStreamer;

    public AnalysisController(AnalysisService analysisService, SseChatStreamer sseChatStreamer) {
        this.analysisService = analysisService;
        this.sseChatStreamer = sseChatStreamer;
    }

    @GetMapping("/api/resumes/{resumeId}/jobs/{jobId}/analysis")
    public SseEmitter analyze(@AuthenticationPrincipal User user,
                              @PathVariable UUID resumeId,
                              @PathVariable UUID jobId) {
        AnalysisService.Prompt prompt = analysisService.prepare(user.getId(), resumeId, jobId);
        return sseChatStreamer.stream(prompt.system(), prompt.user());
    }
}
