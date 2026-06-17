package com.example.ecosystem.nfr;

import com.example.ecosystem.Entity.AuditLog;
import com.example.ecosystem.Entity.Role;
import com.example.ecosystem.Entity.User;
import com.example.ecosystem.repository.AuditLogRepository;
import com.example.ecosystem.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

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
    }
}
