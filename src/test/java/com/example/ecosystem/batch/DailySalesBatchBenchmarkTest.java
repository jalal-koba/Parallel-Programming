package com.example.ecosystem.batch;

import com.example.ecosystem.repository.DailySalesSummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfSystemProperty(named = "heavy", matches = "true")
class DailySalesBatchBenchmarkTest {
    private static final int ORDER_COUNT = 500_000;
    private static final int USERS_COUNT = 5_000;
    private static final int PRODUCTS_COUNT = 1_000;

    @Autowired private JobLauncher jobLauncher;
    @Autowired private Job dailySalesJob;
    @Autowired private LargeBatchDataSeeder seeder;
    @Autowired private DailySalesSummaryRepository summaryRepository;

    @Value("${batch.chunk.size:2000}")
    private int chunkSize;

    @Test
    void batchHandlesFiveHundredThousandOrders() throws Exception {
        LocalDate reportDate = LocalDate.of(2026, 5, 11);
        printHeader(reportDate);

        long seedStart = System.currentTimeMillis();
        seeder.seedCompletedOrders(reportDate, ORDER_COUNT, USERS_COUNT, PRODUCTS_COUNT);
        long seedMs = System.currentTimeMillis() - seedStart;
        BatchConsole.tagf("BENCHMARK", "Seeding phase finished in %s", BatchConsole.highlight(formatDuration(seedMs)));

        long jobStart = System.currentTimeMillis();
        JobExecution execution = jobLauncher.run(dailySalesJob, new JobParametersBuilder()
                .addString("reportDate", reportDate.toString())
                .addLong("run.id", System.nanoTime())
                .toJobParameters());
        long jobMs = System.currentTimeMillis() - jobStart;

        BatchRunReport report = BatchRunReport.from(execution);
        int summaryRows = summaryRepository.findByReportDate(reportDate).size();
        printResults(report, summaryRows, seedMs, jobMs);

        assertThat(report.status()).isEqualTo(BatchStatus.COMPLETED.name());
        assertThat(report.ordersItemsRead()).isEqualTo(ORDER_COUNT);
        assertThat(report.failures()).isZero();
        assertThat(report.chunks()).isGreaterThan(0);
        assertThat(summaryRows).isEqualTo(PRODUCTS_COUNT);
    }

    private void printHeader(LocalDate reportDate) {
        BatchConsole.header("HEAVY BATCH BENCHMARK");
        BatchConsole.line(" Run with: mvn test -Dtest=DailySalesBatchBenchmarkTest -Dheavy=true");
        BatchConsole.tagf("BENCHMARK", "Dataset: %,d completed orders | %,d users | %,d products",
                ORDER_COUNT, USERS_COUNT, PRODUCTS_COUNT);
        BatchConsole.tagf("BENCHMARK", "Report date: %s | chunk size: %,d", reportDate, chunkSize);
        BatchConsole.footer();
    }

    private void printResults(BatchRunReport report, int summaryRows, long seedMs, long jobMs) {
        BatchConsole.header("BENCHMARK RESULTS");
        BatchConsole.metric("Status", BatchConsole.success(report.status()));
        BatchConsole.metric("Seeding time", formatDuration(seedMs));
        BatchConsole.metric("Batch job time", formatDuration(jobMs));
        BatchConsole.metric("Total wall time", formatDuration(seedMs + jobMs));
        BatchConsole.metric("Order items read", String.format("%,d", report.ordersItemsRead()));
        BatchConsole.metric("Order items written", String.format("%,d", report.ordersItemsWritten()));
        BatchConsole.metric("Failures / skipped", String.format("%,d", report.failures()));
        BatchConsole.metric("Chunks committed", String.format("%,d", report.chunks()));
        BatchConsole.metric("Summary rows saved", String.format("%,d", summaryRows));
        if (jobMs > 0) {
            double throughput = report.ordersItemsRead() / (jobMs / 1_000.0);
            BatchConsole.metric("Throughput", String.format("%,.0f items/sec", throughput));
        }
        BatchConsole.footer();
    }

    private static String formatDuration(long millis) {
        if (millis < 1_000) {
            return millis + " ms";
        }
        long seconds = millis / 1_000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        if (minutes == 0) {
            return String.format("%.1f s", millis / 1_000.0);
        }
        return String.format("%dm %02ds", minutes, remainingSeconds);
    }
}
