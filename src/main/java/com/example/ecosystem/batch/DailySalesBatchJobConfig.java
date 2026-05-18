package com.example.ecosystem.batch;

import com.example.ecosystem.Entity.OrderItem;
import com.example.ecosystem.dto.DailySalesItem;
import com.example.ecosystem.repository.OrderItemRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Configuration
@EnableScheduling
public class DailySalesBatchJobConfig {

    @Bean
    @StepScope
    public RepositoryItemReader<OrderItem> orderItemReader(
            OrderItemRepository orderItemRepository,
            @Value("#{jobParameters['reportDate']}") String reportDate,
            @Value("${batch.chunk.size:2000}") int chunkSize
    ) {
        LocalDate date = LocalDate.parse(reportDate);
        return new RepositoryItemReaderBuilder<OrderItem>()
                .name("orderItemReader")
                .repository(orderItemRepository)
                .methodName("findCompletedItemsForDailySales")
                .arguments(List.of(date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .sorts(Map.of("id", Sort.Direction.ASC))
                .pageSize(chunkSize)
                .build();
    }

    @Bean
    public Step dailySalesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            RepositoryItemReader<OrderItem> orderItemReader,
            SalesOrderItemProcessor processor,
            SalesSummaryWriter writer,
            ObjectProvider<BatchChunkProgressListener> progressListener,
            @Value("${batch.chunk.size:2000}") int chunkSize
    ) {
        var stepBuilder = new StepBuilder("dailySalesStep", jobRepository)
                .<OrderItem, DailySalesItem>chunk(chunkSize, transactionManager)
                .reader(orderItemReader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(100);
        progressListener.ifAvailable(stepBuilder::listener);
        return stepBuilder.build();
    }

    @Bean
    public Job dailySalesJob(
            JobRepository jobRepository,
            Step dailySalesStep,
            DailySalesBatchJobListener jobListener
    ) {
        return new JobBuilder("dailySalesJob", jobRepository)
                .listener(jobListener)
                .start(dailySalesStep)
                .build();
    }
}
