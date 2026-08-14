package com.jobmatch.jobstatus.dto;

import java.util.UUID;

/** A job's tracking status for the current user. */
public record JobStatusResponse(UUID jobId, String status) {
}
