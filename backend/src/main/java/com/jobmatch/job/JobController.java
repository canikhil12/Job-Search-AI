package com.jobmatch.job;

import com.jobmatch.job.dto.CreateJobRequest;
import com.jobmatch.job.dto.JobResponse;
import com.jobmatch.job.dto.JobSearchRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final JobSearchService jobSearchService;

    public JobController(JobService jobService, JobSearchService jobSearchService) {
        this.jobService = jobService;
        this.jobSearchService = jobSearchService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> create(@Valid @RequestBody CreateJobRequest request) {
        JobResponse response = jobService.ingest(request, "manual");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Live-search recent postings (via the configured provider), storing new ones. */
    @PostMapping("/search")
    public ResponseEntity<List<JobResponse>> search(@Valid @RequestBody JobSearchRequest request) {
        return ResponseEntity.ok(jobSearchService.search(
                request.query(), request.location(), request.maxDaysOld(), request.limit()));
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> list() {
        return ResponseEntity.ok(jobService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.get(id));
    }
}
