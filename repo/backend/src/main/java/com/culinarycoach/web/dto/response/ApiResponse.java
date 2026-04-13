package com.culinarycoach.web.dto.response;

import com.culinarycoach.audit.TraceContext;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    String traceId,
    boolean success,
    T data,
    ApiError error,
    Map<String, Object> meta
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(TraceContext.get(), true, data, null, null);
    }

    public static <T> ApiResponse<T> ok(T data, Map<String, Object> meta) {
        return new ApiResponse<>(TraceContext.get(), true, data, null, meta);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(TraceContext.get(), false, null,
            new ApiError(code, message, null), null);
    }

    public static <T> ApiResponse<T> fail(String code, String message, Map<String, Object> details) {
        return new ApiResponse<>(TraceContext.get(), false, null,
            new ApiError(code, message, details), null);
    }

    public record ApiError(
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Map<String, Object> details
    ) {}
}
