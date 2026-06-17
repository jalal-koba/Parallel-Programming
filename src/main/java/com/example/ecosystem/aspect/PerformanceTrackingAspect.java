package com.example.ecosystem.aspect;

import com.example.ecosystem.Entity.AuditLog;
import com.example.ecosystem.Entity.User;
import com.example.ecosystem.repository.AuditLogRepository;
import com.example.ecosystem.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.time.LocalDateTime;
import java.util.Map;

@Aspect
@Component
public class PerformanceTrackingAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceTrackingAspect.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public PerformanceTrackingAspect(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object profile(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        
        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            status = "FAILED: " + t.getClass().getSimpleName();
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            try {
                saveAuditLog(joinPoint, duration, status);
            } catch (Exception e) {
                logger.error("Failed to save audit log: {}", e.getMessage());
            }
        }
    }

    private void saveAuditLog(ProceedingJoinPoint joinPoint, long durationMs, String status) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String endpoint = method + " " + uri;

        AuditLog auditLog = new AuditLog();
        auditLog.setEndpoint(endpoint);
        auditLog.setResponseTimeMs((int) durationMs);
        auditLog.setStatus(status);
        auditLog.setTimestamp(LocalDateTime.now());

        // Extract userId from URI path variables
        @SuppressWarnings("unchecked")
        Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pathVariables != null && pathVariables.containsKey("userId")) {
            try {
                Long userId = Long.parseLong(pathVariables.get("userId"));
                User user = userRepository.findById(userId).orElse(null);
                auditLog.setUser(user);
            } catch (NumberFormatException e) {
                // Ignore invalid user id format
            }
        }

        auditLogRepository.save(auditLog);

        logger.info("[PERFORMANCE] Endpoint: '{}' took {} ms | Status: {}", endpoint, durationMs, status);
    }
}
