package com.jobmatch.job.search;

/** Wraps a failure talking to the job-search provider. Mapped to 502. */
public class JobSearchException extends RuntimeException {

    public JobSearchException(String message, Throwable cause) {
        super(message, cause);
    }

    public JobSearchException(String message) {
        super(message);
    }
}
