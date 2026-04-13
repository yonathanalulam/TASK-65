package com.culinarycoach.web.dto.response;

import com.culinarycoach.domain.entity.ScheduledJob;

import java.time.Instant;

public record ScheduledJobResponse(
    Long id,
    String jobName,
    String description,
    boolean enabled,
    String latestRunStatus,
    Instant latestRunAt
) {
    public static ScheduledJobResponse from(ScheduledJob job, String latestRunStatus, Instant latestRunAt) {
        return new ScheduledJobResponse(
            job.getId(),
            job.getJobName(),
            job.getDescription(),
            job.isEnabled(),
            latestRunStatus,
            latestRunAt
        );
    }
}
