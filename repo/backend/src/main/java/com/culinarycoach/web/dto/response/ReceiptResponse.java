package com.culinarycoach.web.dto.response;

import java.time.Instant;

public record ReceiptResponse(
    String receiptNumber,
    Long transactionId,
    Instant generatedAt
) {}
