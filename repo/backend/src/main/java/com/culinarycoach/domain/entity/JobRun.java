package com.culinarycoach.domain.entity;

import com.culinarycoach.domain.enums.JobRunStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "job_runs")
public class JobRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JobRunStatus status = JobRunStatus.QUEUED;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "affected_rows", nullable = false)
    private int affectedRows = 0;

    @Column(name = "affected_files", nullable = false)
    private int affectedFiles = 0;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "trace_id", length = 36)
    private String traceId;

    @Column(name = "checkpoint_data", columnDefinition = "TEXT")
    private String checkpointData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public JobRunStatus getStatus() { return status; }
    public void setStatus(JobRunStatus status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

    public int getAffectedRows() { return affectedRows; }
    public void setAffectedRows(int affectedRows) { this.affectedRows = affectedRows; }

    public int getAffectedFiles() { return affectedFiles; }
    public void setAffectedFiles(int affectedFiles) { this.affectedFiles = affectedFiles; }

    public String getErrorSummary() { return errorSummary; }
    public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getCheckpointData() { return checkpointData; }
    public void setCheckpointData(String checkpointData) { this.checkpointData = checkpointData; }

    public Instant getCreatedAt() { return createdAt; }
}
