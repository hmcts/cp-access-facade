package uk.gov.moj.cpp.audit.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import uk.gov.moj.cpp.audit.config.HttpAuditProperties;
import uk.gov.moj.cpp.audit.core.AsyncAuditChannel;
import uk.gov.moj.cpp.audit.core.AuditRedactor;
import uk.gov.moj.cpp.audit.model.HttpAuditEvent;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "audit.http", name = "enabled", havingValue = "true", matchIfMissing = false)
public class HttpAuditFilter extends OncePerRequestFilter {

    private final HttpAuditProperties props;
    private final AuditRedactor redactor;
    private final AsyncAuditChannel channel;
    @Value("${spring.application.name:app}")
    String service;
    @Value("${app.env:local}")
    String env;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        long start = System.nanoTime();
        String corr = Optional.ofNullable(request.getHeader("X-Correlation-Id"))
                .filter(Predicate.not(String::isBlank))
                .orElse(UUID.randomUUID().toString());
        String userId = Optional.ofNullable(request.getHeader("CPP_USERID")).orElse(null);

        ContentCachingRequestWrapper reqWrap = new ContentCachingRequestWrapper(request, props.getMaxBodyBytes());
        ContentCachingResponseWrapper resWrap = new ContentCachingResponseWrapper(response);

        try {
            chain.doFilter(reqWrap, resWrap);
        } finally {
            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            String reqBody = props.isIncludeRequestBody() ? body(reqWrap.getContentAsByteArray(), request.getCharacterEncoding()) : null;
            String resBody = props.isIncludeResponseBody() ? body(resWrap.getContentAsByteArray(), response.getCharacterEncoding()) : null;

            HttpAuditEvent event = new HttpAuditEvent(
                    "1.0",
                    UUID.randomUUID().toString(),
                    service,
                    env,
                    Instant.now(),
                    corr,
                    userId,
                    request.getMethod(),
                    request.getRequestURI(),
                    Optional.ofNullable(request.getQueryString()).orElse(""),
                    resWrap.getStatus(),
                    latencyMs,
                    clientIp(request),
                    Optional.ofNullable(request.getHeader("User-Agent")).orElse(""),
                    Optional.ofNullable(request.getContentLengthLong()).orElse(0L),
                    resWrap.getContentSize(),
                    redactor.redactHeaders(request),
                    props.isIncludeRequestBody() ? redactor.redactBody(reqBody) : null,
                    props.isIncludeResponseBody() ? redactor.redactBody(resBody) : null
            );

            try {
                channel.submit(event);
            } catch (Exception e) {
                if (props.getFailurePolicy() == HttpAuditProperties.FailurePolicy.FAIL_CLOSED) {
                    throw new ServletException("Audit pipeline failed", e);
                }
            } finally {
                resWrap.copyBodyToResponse();
            }
        }
    }

    private String body(byte[] bytes, @Nullable String enc) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            return new String(bytes, enc != null ? Charset.forName(enc) : StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private String clientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
        return Optional.ofNullable(req.getRemoteAddr()).orElse("unknown");
    }
}
