package com.example.ecosystem.listener;

import com.example.ecosystem.event.OrderCreatedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderCreatedListener {

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {

        System.out.println("=================================");
        System.out.println("ASYNC TASK STARTED");
        System.out.println("Thread: " + Thread.currentThread().getName());
        System.out.println("Processing order: " + event.getOrderId());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Invoice generated");
        System.out.println("Email sent");
        System.out.println("Audit log saved");
        System.out.println("=================================");
    }
}