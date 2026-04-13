package com.culinarycoach.service;

import com.culinarycoach.audit.TraceContext;
import com.culinarycoach.domain.entity.JobRun;
import com.culinarycoach.domain.entity.ScheduledJob;
import com.culinarycoach.domain.enums.AlertSeverity;
import com.culinarycoach.domain.enums.JobRunStatus;
import com.culinarycoach.domain.repository.JobRunRepository;
import com.culinarycoach.domain.repository.ScheduledJobRepository;
import com.culinarycoach.web.dto.response.JobRunResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class JobRunService {

    private static final Logger log = LoggerFactory.getLogger(JobRunService.class);

    private final ScheduledJobRepository scheduledJobRepository;
    private final JobRunRepository jobRunRepository;
    private final AnomalyAlertService anomalyAlertService;

    public JobRunService(ScheduledJobRepository scheduledJobRepository,
                         JobRunRepository jobRunRepository,
                         AnomalyAlertService anomalyAlertService) {
        this.scheduledJobRepository = scheduledJobRepository;
        this.jobRunRepository = jobRunRepository;
        this.anomalyAlertService = anomalyAlertService;
    }

    @Transactional
    public ScheduledJob registerJob(String jobName, String description, String cronExpression) {
        Optional<ScheduledJob> existing = scheduledJobRepository.findByJobName(jobName);
        if (existing.isPresent()) {
            return existing.get();
        }

        ScheduledJob job = new ScheduledJob();
        job.setJobName(jobName);
        job.setDescription(description);
        job.setCronExpression(cronExpression);
        job.setEnabled(true);
        return scheduledJobRepository.save(job);
    }

    @Transactional
    public JobRun startRun(String jobName) {
        ScheduledJob job = scheduledJobRepository.findByJobName(jobName)
            .orElseThrow(() -> new IllegalArgumentException("Unknown job: " + jobName));

        JobRun run = new JobRun();
        run.setJobId(job.getId());
        run.setStatus(JobRunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        run.setTraceId(TraceContext.get());
        return jobRunRepository.save(run);
    }

    @Transactional
    public JobRun completeRun(Long runId, int affectedRows, int affectedFiles) {
        JobRun run = jobRunRepository.findById(runId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown run: " + runId));

        run.setStatus(JobRunStatus.SUCCEEDED);
        run.setEndedAt(Instant.now());
        run.setAffectedRows(affectedRows);
        run.setAffectedFiles(affectedFiles);
        return jobRunRepository.save(run);
    }

    @Transactional
    public JobRun failRun(Long runId, String errorSummary) {
        JobRun run = jobRunRepository.findById(runId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown run: " + runId));

        ScheduledJob job = scheduledJobRepository.findById(run.getJobId())
            .orElseThrow(() -> new IllegalStateException("Job not found for run: " + runId));

        run.setEndedAt(Instant.now());
        run.setErrorSummary(errorSummary);

        if (run.getRetryCount() < job.getMaxRetries()) {
            run.setStatus(JobRunStatus.RETRY_QUEUED);
            run.setRetryCount(run.getRetryCount() + 1);
            log.warn("Job run {} failed, retry queued ({}/{})", runId,
                run.getRetryCount(), job.getMaxRetries());
        } else {
            run.setStatus(JobRunStatus.TERMINAL_FAILED);
            log.error("Job run {} terminal failure after {} retries: {}",
                runId, run.getRetryCount(), errorSummary);

            // Create critical alert for terminal failures
            anomalyAlertService.createAlert(
                "JOB_TERMINAL_FAILURE",
                AlertSeverity.CRITICAL,
                "Job '" + job.getJobName() + "' failed terminally after "
                    + job.getMaxRetries() + " retries: " + errorSummary,
                "job.failure." + job.getJobName(),
                null,
                null
            );
        }

        return jobRunRepository.save(run);
    }

    @Transactional
    public JobRun partialSuccess(Long runId, int affectedRows, String errorSummary) {
        JobRun run = jobRunRepository.findById(runId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown run: " + runId));

        run.setStatus(JobRunStatus.PARTIAL_SUCCESS);
        run.setEndedAt(Instant.now());
        run.setAffectedRows(affectedRows);
        run.setErrorSummary(errorSummary);
        return jobRunRepository.save(run);
    }

    @Transactional
    public JobRun cancelRun(Long runId) {
        JobRun run = jobRunRepository.findById(runId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown run: " + runId));

        if (run.getStatus() != JobRunStatus.QUEUED && run.getStatus() != JobRunStatus.RETRY_QUEUED) {
            throw new IllegalStateException("Can only cancel QUEUED or RETRY_QUEUED runs, current: "
                + run.getStatus());
        }

        run.setStatus(JobRunStatus.CANCELLED);
        run.setEndedAt(Instant.now());
        return jobRunRepository.save(run);
    }

    public Page<JobRunResponse> listRuns(String jobName, Pageable pageable) {
        ScheduledJob job = scheduledJobRepository.findByJobName(jobName)
            .orElseThrow(() -> new IllegalArgumentException("Unknown job: " + jobName));

        return jobRunRepository.findByJobIdOrderByStartedAtDesc(job.getId(), pageable)
            .map(run -> JobRunResponse.from(run, job.getJobName()));
    }

    public Optional<JobRun> getLatestRun(String jobName) {
        return scheduledJobRepository.findByJobName(jobName)
            .flatMap(job -> {
                List<JobRun> runs = jobRunRepository.findLatestByJobId(
                    job.getId(), PageRequest.of(0, 1));
                return runs.isEmpty() ? Optional.empty() : Optional.of(runs.get(0));
            });
    }

    public long countFailuresSince(Long jobId, Instant since) {
        return jobRunRepository.countByJobIdAndStatusAndCreatedAtAfter(
            jobId, JobRunStatus.FAILED, since)
            + jobRunRepository.countByJobIdAndStatusAndCreatedAtAfter(
            jobId, JobRunStatus.TERMINAL_FAILED, since);
    }
}
