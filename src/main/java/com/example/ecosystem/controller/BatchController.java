package com.example.ecosystem.controller;

import com.example.ecosystem.Entity.DailySalesSummary;
import com.example.ecosystem.batch.BatchRunReport;
import com.example.ecosystem.repository.DailySalesSummaryRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/batch")
public class BatchController {

    public record JobHistoryEntry(
            Long executionId,
            String status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String reportDate,
            long durationMs
    ) {}

    private final JobLauncher jobLauncher;
    private final Job dailySalesJob;
    private final DailySalesSummaryRepository summaryRepository;
    private final JobExplorer jobExplorer;

    public BatchController(
            JobLauncher jobLauncher,
            Job dailySalesJob,
            DailySalesSummaryRepository summaryRepository,
            JobExplorer jobExplorer
    ) {
        this.jobLauncher = jobLauncher;
        this.dailySalesJob = dailySalesJob;
        this.summaryRepository = summaryRepository;
        this.jobExplorer = jobExplorer;
    }

    @PostMapping("/daily-sales")
    public BatchRunReport runDailySales(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) throws Exception {
        LocalDate targetDate = date == null ? LocalDate.now().minusDays(1) : date;
        JobExecution execution = jobLauncher.run(dailySalesJob, new JobParametersBuilder()
                .addString("reportDate", targetDate.toString())
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters());
        return BatchRunReport.from(execution);
    }

    @GetMapping("/daily-sales/summary")
    public List<DailySalesSummary> getSummary(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        LocalDate targetDate = date == null ? LocalDate.now().minusDays(1) : date;
        return summaryRepository.findByReportDate(targetDate);
    }

    @GetMapping("/job-history")
    public List<JobHistoryEntry> getJobHistory() {
        List<JobHistoryEntry> history = new ArrayList<>();
        for (var instance : jobExplorer.getJobInstances("dailySalesJob", 0, 20)) {
            for (JobExecution execution : jobExplorer.getJobExecutions(instance)) {
                long durationMs = (execution.getStartTime() != null && execution.getEndTime() != null)
                        ? Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis()
                        : 0;
                history.add(new JobHistoryEntry(
                        execution.getId(),
                        execution.getStatus().name(),
                        execution.getStartTime(),
                        execution.getEndTime(),
                        execution.getJobParameters().getString("reportDate"),
                        durationMs
                ));
            }
        }
        return history;
    }
}
