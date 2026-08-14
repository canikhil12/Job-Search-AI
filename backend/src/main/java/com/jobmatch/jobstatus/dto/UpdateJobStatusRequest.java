package com.jobmatch.jobstatus.dto;

import jakarta.validation.constraints.Pattern;

public record UpdateJobStatusRequest(
        @Pattern(regexp = "saved|applied", message = "status must be 'saved' or 'applied'")
        String status
) {
}
