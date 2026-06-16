package com.example.ecosystem.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DailySalesBatchJob {
    private static final Logger logger = LoggerFactory.getLogger(DailySalesBatchJob.class);

    private final JobLauncher jobLauncher;
    private final Job dailySalesJob;

    public DailySalesBatchJob(JobLauncher jobLauncher, Job dailySalesJob) {
        this.jobLauncher = jobLauncher;
        this.dailySalesJob = dailySalesJob;
    }

    @Scheduled(cron = "${batch.daily.cron:0 0 1 * * *}")
    public void runYesterdaySalesBatch() throws Exception {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        JobExecution execution = jobLauncher.run(dailySalesJob, new JobParametersBuilder()
                .addString("reportDate", targetDate.toString())
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters());
        logger.info("Daily sales batch finished for {} -> {}", targetDate, BatchRunReport.from(execution));
    }
}
