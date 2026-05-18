package com.example.ecosystem.batch;

import com.example.ecosystem.repository.DailySalesSummaryRepository;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DailySalesBatchJobListener implements JobExecutionListener {

    private final DailySalesSummaryRepository summaryRepository;

    public DailySalesBatchJobListener(DailySalesSummaryRepository summaryRepository) {
        this.summaryRepository = summaryRepository;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        boolean isRestart = !jobExecution.getStepExecutions().isEmpty();
        if (isRestart) {
            return;
        }
        LocalDate reportDate = LocalDate.parse(jobExecution.getJobParameters().getString("reportDate"));
        summaryRepository.deleteByReportDate(reportDate);
    }
}
