package com.example.ecosystem.nfr;

import com.example.ecosystem.Entity.AuditLog;
import com.example.ecosystem.Entity.Role;
import com.example.ecosystem.Entity.User;
import com.example.ecosystem.dto.ProductRequest;
import com.example.ecosystem.repository.AuditLogRepository;
import com.example.ecosystem.repository.ProductRepository;
import com.example.ecosystem.repository.UserRepository;
import com.example.ecosystem.service.ProductService;
import com.example.ecosystem.batch.BatchConsole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("nfr10")
class Nfr10PerformanceTrackingTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;


    @Test
    void controllerEndpointsAreTrackedByAopAndSavedToDatabase() throws Exception {
        auditLogRepository.deleteAll();

        // 1. Test public endpoint tracking
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).isNotEmpty();
        AuditLog publicLog = logs.stream()
                .filter(l -> l.getEndpoint().equals("GET /api/products"))
                .findFirst()
                .orElse(null);

        assertThat(publicLog).isNotNull();
        assertThat(publicLog.getResponseTimeMs()).isGreaterThanOrEqualTo(0);
        assertThat(publicLog.getStatus()).isEqualTo("SUCCESS");
        assertThat(publicLog.getUser()).isNull();

        // 2. Test user-context endpoint tracking
        User user = new User();
        user.setUsername("perf-user");
        user.setEmail("perf-user@example.com");
        user.setPassword("pass");
        user.setRole(Role.customer);
        user = userRepository.save(user);

        mockMvc.perform(get("/api/users/" + user.getId() + "/cart"))
                .andExpect(status().isOk());

        logs = auditLogRepository.findAll();
        AuditLog userLog = logs.stream()
                .filter(l -> l.getEndpoint().contains("/cart"))
                .findFirst()
                .orElse(null);

        assertThat(userLog).isNotNull();
        assertThat(userLog.getUser()).isNotNull();
        assertThat(userLog.getUser().getId()).isEqualTo(user.getId());

        // Dynamic console output of real measured metrics in English to avoid Windows encoding issues
        BatchConsole.header("NFR 10: PERFORMANCE METRICS CAPTURED BY AOP ASPECT");
        BatchConsole.metric("Endpoint 1", publicLog.getEndpoint());
        BatchConsole.metric("Response Time 1", publicLog.getResponseTimeMs() + " ms");
        BatchConsole.metric("Status 1", BatchConsole.success(publicLog.getStatus()));
        
        BatchConsole.metric("Endpoint 2", userLog.getEndpoint());
        BatchConsole.metric("Response Time 2", userLog.getResponseTimeMs() + " ms");
        BatchConsole.metric("Status 2", BatchConsole.success(userLog.getStatus()));
        BatchConsole.metric("Associated User ID", String.valueOf(userLog.getUser().getId()));
        
        BatchConsole.metric("Audit Logs Saved", String.valueOf(logs.size()));
        BatchConsole.footer();

        // 3. Bottleneck Identification & Benchmark Comparison for Requirement 10
        // Clean database state before benchmarking
        productRepository.deleteAll();

        int batchSize = 100; // 100 products is a good size for measuring transaction commit overhead in a quick JUnit test
        List<ProductRequest> requests = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            requests.add(new ProductRequest("Product-" + i, "Desc-" + i, 10.0f + i, 100 + i, null));
        }

        // Measure Before Optimization: individual commits (no active transaction in service method)
        long startBefore = System.currentTimeMillis();
        productService.createProductsNonTransactional(requests);
        long timeBefore = System.currentTimeMillis() - startBefore;
        assertThat(productRepository.count()).isEqualTo(batchSize);

        // Clean database state for the next run
        productRepository.deleteAll();

        // Measure After Optimization: single transaction commit (annotated with @Transactional)
        long startAfter = System.currentTimeMillis();
        productService.createProductsTransactional(requests);
        long timeAfter = System.currentTimeMillis() - startAfter;
        assertThat(productRepository.count()).isEqualTo(batchSize);

        // Clean up database state after benchmark to avoid pollution
        productRepository.deleteAll();

        double speedup = (double) timeBefore / Math.max(1, timeAfter);

        BatchConsole.header("NFR 10: BOTTLENECK IDENTIFICATION & BENCHMARK COMPARISON");
        BatchConsole.metric("Identified Bottleneck", "Database I/O Transaction Commits (Individual Saves vs Single Transaction Commit)");
        BatchConsole.metric("Before Optimization", String.format("Non-Transactional Batch (%d items) | Time: %d ms", batchSize, timeBefore));
        BatchConsole.metric("After Optimization", String.format("Transactional Batch (%d items) | Time: %d ms", batchSize, timeAfter));
        BatchConsole.metric("Performance Gain", String.format("%.2fx Speedup (Reduced total commits from %d to 1)", speedup, batchSize));
        BatchConsole.footer();
    }
}
