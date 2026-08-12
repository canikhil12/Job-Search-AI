package com.jobmatch.resume.dto;

/** Raw file payload for the download endpoint. */
public record ResumeDownload(String fileName, String contentType, byte[] content) {
}
