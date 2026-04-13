package com.culinarycoach.web.dto.response;

import java.math.BigDecimal;

public record ProductBundleResponse(
    Long id,
    String name,
    String description,
    BigDecimal price
) {}
