package com.example.ecosystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // عدد الخيوط الأساسية
        executor.setCorePoolSize(4);

        // الحد الأقصى للخيوط
        executor.setMaxPoolSize(8);

        // حجم الانتظار للمهام
        executor.setQueueCapacity(100);

        // اسم الخيوط للتتبع
        executor.setThreadNamePrefix("Async-Executor-");

        executor.initialize();

        return executor;
    }
}