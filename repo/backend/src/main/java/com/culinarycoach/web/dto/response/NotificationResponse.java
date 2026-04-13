package com.culinarycoach.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResponse(
    Long id,
    String type,
    String title,
    String message,
    String status,
    int priority,
    Instant createdAt
) {}
