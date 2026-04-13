package com.culinarycoach.service;

import com.culinarycoach.audit.AuditEventType;
import com.culinarycoach.audit.TraceContext;
import com.culinarycoach.domain.entity.AuditLog;
import com.culinarycoach.domain.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    public void log(AuditEventType eventType, Long userId, String username,
                    String ipAddress, String userAgent, String details) {
        log(eventType, userId, username, ipAddress, userAgent, null, null, details);
    }

    @Async
    public void log(AuditEventType eventType, Long userId, String username,
                    String ipAddress, String userAgent,
                    String resourceType, String resourceId, String details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setEventType(eventType.name());
            entry.setUserId(userId);
            entry.setUsername(username);
            entry.setIpAddress(ipAddress);
            entry.setUserAgent(userAgent);
            entry.setResourceType(resourceType);
            entry.setResourceId(resourceId);
            entry.setDetails(details);
            entry.setTraceId(TraceContext.get());
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log: event={}, user={}, trace={}",
                eventType, username, TraceContext.get(), e);
        }
    }

    public void logSync(AuditEventType eventType, Long userId, String username,
                        String ipAddress, String userAgent, String details) {
        AuditLog entry = new AuditLog();
        entry.setEventType(eventType.name());
        entry.setUserId(userId);
        entry.setUsername(username);
        entry.setIpAddress(ipAddress);
        entry.setUserAgent(userAgent);
        entry.setTraceId(TraceContext.get());
        entry.setDetails(details);
        auditLogRepository.save(entry);
    }
}
