package uk.gov.moj.cpp.audit.model;

public record HttpAuditEvent(
        String eventVersion,
        String eventId,
        String service,
        String env,
        java.time.Instant timestamp,
        String correlationId,
        String userId,
        String method,
        String path,
        String query,
        int status,
        long latencyMs,
        String clientIp,
        String userAgent,
        long requestBytes,
        long responseBytes,
        java.util.Map<String, String> headers,
        String requestBody,
        String responseBody
) implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
}

