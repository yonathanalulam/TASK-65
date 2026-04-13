package com.culinarycoach.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReconciliationExportResponse(
    Long id,
    LocalDate businessDate,
    int exportVersion,
    String filePath,
    String fileChecksum,
    int transactionCount,
    BigDecimal totalCompletedAmount,
    BigDecimal totalVoidedAmount,
    String generatedBy,
    Instant generatedAt
) {}
