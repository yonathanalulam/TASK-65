package com.culinarycoach.audit;

import org.slf4j.MDC;

import java.util.UUID;

public final class TraceContext {

    private static final String TRACE_ID_KEY = "traceId";
    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private TraceContext() {}

    public static String get() {
        String id = HOLDER.get();
        return id != null ? id : "unknown";
    }

    public static String init() {
        String id = UUID.randomUUID().toString();
        set(id);
        return id;
    }

    public static void set(String traceId) {
        HOLDER.set(traceId);
        MDC.put(TRACE_ID_KEY, traceId);
    }

    public static void clear() {
        HOLDER.remove();
        MDC.remove(TRACE_ID_KEY);
    }
}
