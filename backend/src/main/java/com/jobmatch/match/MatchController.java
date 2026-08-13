package com.jobmatch.match;

import com.jobmatch.job.dto.JobMatchResponse;
import com.jobmatch.match.dto.MatchRequest;
import com.jobmatch.user.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    /** Match arbitrary pasted text against the job corpus. */
    @PostMapping("/api/matches")
    public ResponseEntity<List<JobMatchResponse>> matchText(@Valid @RequestBody MatchRequest request) {
        return ResponseEntity.ok(matchService.matchText(request.text(), request.limit()));
    }

    /** Match one of the caller's résumés against the job corpus. */
    @GetMapping("/api/resumes/{id}/matches")
    public ResponseEntity<List<JobMatchResponse>> matchResume(@AuthenticationPrincipal User user,
                                                              @PathVariable UUID id,
                                                              @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(matchService.matchResume(user.getId(), id, limit));
    }
}
