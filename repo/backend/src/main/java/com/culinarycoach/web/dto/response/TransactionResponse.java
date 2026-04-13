package com.culinarycoach.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TransactionResponse(
    Long id,
    String status,
    BigDecimal totalAmount,
    String receiptNumber,
    Instant initiatedAt,
    Instant completedAt,
    List<TransactionItemResponse> items,
    String paymentRefMasked
) {}
