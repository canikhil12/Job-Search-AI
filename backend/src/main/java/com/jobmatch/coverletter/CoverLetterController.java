package com.jobmatch.coverletter;

import com.jobmatch.chat.SseChatStreamer;
import com.jobmatch.user.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * Streams a tailored cover letter as Server-Sent Events. Same shape as the gap-analysis endpoint:
 * validate résumé/job synchronously (404 before streaming), then relay Claude's tokens.
 */
@RestController
public class CoverLetterController {

    private final CoverLetterService coverLetterService;
    private final SseChatStreamer sseChatStreamer;

    public CoverLetterController(CoverLetterService coverLetterService, SseChatStreamer sseChatStreamer) {
        this.coverLetterService = coverLetterService;
        this.sseChatStreamer = sseChatStreamer;
    }

    @GetMapping("/api/resumes/{resumeId}/jobs/{jobId}/cover-letter")
    public SseEmitter coverLetter(@AuthenticationPrincipal User user,
                                  @PathVariable UUID resumeId,
                                  @PathVariable UUID jobId) {
        CoverLetterService.Prompt prompt = coverLetterService.prepare(user.getId(), resumeId, jobId);
        return sseChatStreamer.stream(prompt.system(), prompt.user());
    }
}
