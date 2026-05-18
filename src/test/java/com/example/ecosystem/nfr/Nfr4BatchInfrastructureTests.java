package com.example.ecosystem.nfr;

import com.example.ecosystem.batch.DailySalesBatchJobConfig;
import com.example.ecosystem.batch.SalesOrderItemProcessor;
import com.example.ecosystem.batch.SalesSummaryWriter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NFR #4 — Spring Batch wiring (job, step, components). Correctness/load: {@code DailySalesBatchJobTest}.
 */
@SpringBootTest
@Tag("nfr4")
class Nfr4BatchInfrastructureTests {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Job dailySalesJob;

    @Value("${batch.chunk.size}")
    private int configuredChunkSize;

    @Test
    void batchInfrastructureBeansAreRegistered() {
        assertThat(context.getBean(JobLauncher.class)).isNotNull();
        assertThat(context.getBean(JobRepository.class)).isNotNull();
        assertThat(context.getBean(DailySalesBatchJobConfig.class)).isNotNull();
        assertThat(context.getBean(SalesOrderItemProcessor.class)).isNotNull();
        assertThat(context.getBean(SalesSummaryWriter.class)).isNotNull();
    }

    @Test
    void dailySalesJobAndStepExist() {
        assertThat(dailySalesJob.getName()).isEqualTo("dailySalesJob");

        Step step = context.getBean("dailySalesStep", Step.class);
        assertThat(step.getName()).isEqualTo("dailySalesStep");
    }

    @Test
    void chunkSizeIsConfiguredForTests() {
        assertThat(configuredChunkSize).isEqualTo(200);
    }
}
