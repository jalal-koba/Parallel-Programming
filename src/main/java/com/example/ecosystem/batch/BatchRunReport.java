package com.example.ecosystem.batch;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

import java.time.Duration;

public record BatchRunReport(
        String status,
        long durationMs,
        long ordersItemsRead,
        long ordersItemsWritten,
        long failures,
        long chunks
) {
    public static BatchRunReport from(JobExecution execution) {
        long durationMs = (execution.getStartTime() != null && execution.getEndTime() != null)
                ? Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis()
                : 0;
        long read = 0, written = 0, skipped = 0, commits = 0;
        for (StepExecution step : execution.getStepExecutions()) {
            read += step.getReadCount();
            written += step.getWriteCount();
            skipped += step.getSkipCount();
            commits += step.getCommitCount();
        }
        return new BatchRunReport(
                execution.getStatus() == null ? BatchStatus.UNKNOWN.name() : execution.getStatus().name(),
                durationMs,
                read,
                written,
                skipped,
                commits
        );
    }
}
