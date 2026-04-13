package com.culinarycoach.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record CapacityReportResponse(
    long totalUsers,
    long activeSessions,
    BigDecimal totalAudioCacheBytes,
    long totalTransactions,
    long pendingNotifications,
    Instant reportTime
) {}
