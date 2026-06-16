package com.example.ecosystem.batch;

import com.example.ecosystem.Entity.OrderItem;
import com.example.ecosystem.dto.DailySalesItem;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SalesOrderItemProcessor implements ItemProcessor<OrderItem, DailySalesItem> {

    @Override
    public DailySalesItem process(OrderItem item) {
        if (item.getProduct() == null) {
            return null;
        }
        BigDecimal unitPrice = BigDecimal.valueOf(item.getUnitPriceAtPurchase());
        BigDecimal revenue = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        return new DailySalesItem(
                item.getOrder().getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                revenue
        );
    }
}
