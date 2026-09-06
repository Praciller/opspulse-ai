package com.opspulse.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final int MAX_REQUEST_ID_LENGTH = 64;
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]+");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        var requestId = resolveRequestId(request.getHeader(RequestIdContext.HEADER_NAME));

        MDC.put(RequestIdContext.MDC_KEY, requestId);
        response.setHeader(RequestIdContext.HEADER_NAME, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RequestIdContext.USER_ID_MDC_KEY);
            MDC.remove(RequestIdContext.MDC_KEY);
        }
    }

    private String resolveRequestId(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return UUID.randomUUID().toString();
        }

        var normalized = candidate.trim();
        if (normalized.length() > MAX_REQUEST_ID_LENGTH
                || !SAFE_REQUEST_ID.matcher(normalized).matches()) {
            return UUID.randomUUID().toString();
        }

        return normalized;
    }
}
