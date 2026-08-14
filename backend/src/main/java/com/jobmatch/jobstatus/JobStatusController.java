package com.jobmatch.jobstatus;

import com.jobmatch.common.ResourceNotFoundException;
import com.jobmatch.job.JobRepository;
import com.jobmatch.jobstatus.dto.JobStatusResponse;
import com.jobmatch.jobstatus.dto.UpdateJobStatusRequest;
import com.jobmatch.user.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class JobStatusController {

    private final JobStatusRepository jobStatusRepository;
    private final JobRepository jobRepository;

    public JobStatusController(JobStatusRepository jobStatusRepository, JobRepository jobRepository) {
        this.jobStatusRepository = jobStatusRepository;
        this.jobRepository = jobRepository;
    }

    /** All of the current user's saved/applied statuses (for board badges + the tracker view). */
    @GetMapping("/api/jobs/statuses")
    public ResponseEntity<List<JobStatusResponse>> myStatuses(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(jobStatusRepository.findByUser(user.getId()));
    }

    @PutMapping("/api/jobs/{jobId}/status")
    public ResponseEntity<Void> setStatus(@AuthenticationPrincipal User user,
                                          @PathVariable UUID jobId,
                                          @Valid @RequestBody UpdateJobStatusRequest request) {
        if (!jobRepository.existsById(jobId)) {
            throw new ResourceNotFoundException("Job not found: " + jobId);
        }
        jobStatusRepository.upsert(user.getId(), jobId, request.status());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/jobs/{jobId}/status")
    public ResponseEntity<Void> clearStatus(@AuthenticationPrincipal User user, @PathVariable UUID jobId) {
        jobStatusRepository.delete(user.getId(), jobId);
        return ResponseEntity.noContent().build();
    }
}
