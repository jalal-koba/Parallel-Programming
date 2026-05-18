package com.example.ecosystem.nfr;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NFR #2 — HikariCP connection pool configuration and basic concurrent borrow behaviour.
 */
@SpringBootTest
@Tag("nfr2")
class Nfr2HikariPoolTests {

    @Autowired
    private DataSource dataSource;

    @Value("${spring.datasource.hikari.maximum-pool-size}")
    private int configuredMaxPoolSize;

    @Test
    void dataSourceIsHikariWithConfiguredMaxPoolSize() {
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);

        HikariDataSource hikari = (HikariDataSource) dataSource;
        assertThat(hikari.getMaximumPoolSize()).isEqualTo(configuredMaxPoolSize);
        assertThat(hikari.getPoolName()).isNotBlank();
    }

    @Test
    void poolSupportsConcurrentConnectionsUpToMaxSize() throws Exception {
        int borrowCount = Math.max(2, configuredMaxPoolSize);
        ExecutorService executor = Executors.newFixedThreadPool(borrowCount);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < borrowCount; i++) {
            futures.add(executor.submit(() -> {
                try (Connection connection = dataSource.getConnection()) {
                    return connection.isValid(2);
                }
            }));
        }

        for (Future<Boolean> future : futures) {
            assertThat(future.get()).isTrue();
        }
        executor.shutdown();
    }
}
