package com.jobmatch.tailor;

import com.jobmatch.chat.SseChatStreamer;
import com.jobmatch.user.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/** Streams a job-tailored résumé rewrite as Server-Sent Events. */
@RestController
public class TailorController {

    private final TailorService tailorService;
    private final SseChatStreamer sseChatStreamer;

    public TailorController(TailorService tailorService, SseChatStreamer sseChatStreamer) {
        this.tailorService = tailorService;
        this.sseChatStreamer = sseChatStreamer;
    }

    @GetMapping("/api/resumes/{resumeId}/jobs/{jobId}/tailor")
    public SseEmitter tailor(@AuthenticationPrincipal User user,
                             @PathVariable UUID resumeId,
                             @PathVariable UUID jobId) {
        TailorService.Prompt prompt = tailorService.prepare(user.getId(), resumeId, jobId);
        return sseChatStreamer.stream(prompt.system(), prompt.user());
    }
}
