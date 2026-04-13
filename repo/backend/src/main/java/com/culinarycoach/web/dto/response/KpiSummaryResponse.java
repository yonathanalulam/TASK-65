package com.culinarycoach.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record KpiSummaryResponse(
    BigDecimal requestThroughput,
    BigDecimal errorRate,
    BigDecimal p50Latency,
    BigDecimal p95Latency,
    Instant windowStart,
    Instant windowEnd
) {}
