package com.culinarycoach.web.dto.response;

import java.math.BigDecimal;

public record TransactionItemResponse(
    Long bundleId,
    String bundleName,
    BigDecimal unitPrice,
    int quantity,
    BigDecimal lineTotal
) {}
