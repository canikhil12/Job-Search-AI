package com.jobmatch.ats;

import com.jobmatch.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class AtsController {

    private final AtsService atsService;

    public AtsController(AtsService atsService) {
        this.atsService = atsService;
    }

    @GetMapping("/api/resumes/{resumeId}/jobs/{jobId}/ats")
    public ResponseEntity<AtsResult> ats(@AuthenticationPrincipal User user,
                                         @PathVariable UUID resumeId,
                                         @PathVariable UUID jobId) {
        return ResponseEntity.ok(atsService.score(user.getId(), resumeId, jobId));
    }
}
