package com.jobmatch.resume;

import com.jobmatch.resume.dto.ResumeDetailResponse;
import com.jobmatch.resume.dto.ResumeDownload;
import com.jobmatch.resume.dto.ResumeResponse;
import com.jobmatch.user.User;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeDetailResponse> upload(@AuthenticationPrincipal User user,
                                                       @RequestParam("file") MultipartFile file) {
        ResumeDetailResponse response = resumeService.upload(user.getId(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ResumeResponse>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(resumeService.list(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeDetailResponse> get(@AuthenticationPrincipal User user,
                                                    @PathVariable UUID id) {
        return ResponseEntity.ok(resumeService.get(user.getId(), id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal User user,
                                             @PathVariable UUID id) {
        ResumeDownload file = resumeService.download(user.getId(), id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(new ByteArrayResource(file.content()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user,
                                       @PathVariable UUID id) {
        resumeService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
