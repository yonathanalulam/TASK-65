package com.culinarycoach.web.dto.response;

import com.culinarycoach.domain.entity.JobRun;

import java.time.Instant;

public record JobRunResponse(
    Long id,
    String jobName,
    String status,
    Instant startedAt,
    Instant endedAt,
    int affectedRows,
    int affectedFiles,
    String errorSummary,
    int retryCount,
    String traceId
) {
    public static JobRunResponse from(JobRun run, String jobName) {
        return new JobRunResponse(
            run.getId(),
            jobName,
            run.getStatus().name(),
            run.getStartedAt(),
            run.getEndedAt(),
            run.getAffectedRows(),
            run.getAffectedFiles(),
            run.getErrorSummary(),
            run.getRetryCount(),
            run.getTraceId()
        );
    }
}
