package com.culinarycoach.web.dto.response;

public record StorageMeterResponse(
    long usedBytes,
    long totalQuotaBytes,
    double percentUsed,
    long reclaimableBytes
) {}
