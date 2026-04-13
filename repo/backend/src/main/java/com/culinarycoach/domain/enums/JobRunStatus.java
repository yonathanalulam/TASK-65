package com.culinarycoach.domain.enums;

public enum JobRunStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    PARTIAL_SUCCESS,
    RETRY_QUEUED,
    TERMINAL_FAILED,
    CANCELLED
}
