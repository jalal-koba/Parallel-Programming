package com.example.ecosystem.batch;

import com.example.ecosystem.Entity.DailySalesSummary;
import com.example.ecosystem.dto.DailySalesItem;
import com.example.ecosystem.repository.DailySalesSummaryRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@StepScope
public class SalesSummaryWriter implements ItemWriter<DailySalesItem> {

    private final DailySalesSummaryRepository summaryRepository;

    @Value("#{jobParameters['reportDate']}")
    private String reportDateRaw;

    public SalesSummaryWriter(DailySalesSummaryRepository summaryRepository) {
        this.summaryRepository = summaryRepository;
    }

    @Override
    public void write(Chunk<? extends DailySalesItem> chunk) {
        Map<Long, ChunkRollup> rollups = new HashMap<>();
        for (DailySalesItem salesItem : chunk.getItems()) {
            rollups.computeIfAbsent(
                    salesItem.productId(),
                    id -> new ChunkRollup(salesItem.productName())
            ).add(salesItem);
        }

        LocalDate reportDate = LocalDate.parse(reportDateRaw);
        LocalDateTime generatedAt = LocalDateTime.now();

        for (Map.Entry<Long, ChunkRollup> entry : rollups.entrySet()) {
            Long productId = entry.getKey();
            ChunkRollup rollup = entry.getValue();
            DailySalesSummary summary = summaryRepository
                    .findByReportDateAndProductId(reportDate, productId)
                    .orElseGet(DailySalesSummary::new);

            if (summary.getId() == null) {
                summary.setReportDate(reportDate);
                summary.setProductId(productId);
                summary.setProductName(rollup.productName);
                summary.setTotalQuantitySold(0L);
                summary.setTotalRevenue(BigDecimal.ZERO);
                summary.setOrdersCount(0L);
            }

            summary.setTotalQuantitySold(summary.getTotalQuantitySold() + rollup.quantity);
            summary.setTotalRevenue(summary.getTotalRevenue().add(rollup.revenue));
            summary.setOrdersCount(summary.getOrdersCount() + rollup.orderIds.size());
            summary.setGeneratedAt(generatedAt);
            summaryRepository.save(summary);
        }
    }

    private static final class ChunkRollup {
        private final String productName;
        private long quantity;
        private BigDecimal revenue = BigDecimal.ZERO;
        private final Set<Long> orderIds = new HashSet<>();

        private ChunkRollup(String productName) {
            this.productName = productName;
        }

        private void add(DailySalesItem salesItem) {
            quantity += salesItem.quantity();
            revenue = revenue.add(salesItem.revenue());
            orderIds.add(salesItem.orderId());
        }
    }
}
