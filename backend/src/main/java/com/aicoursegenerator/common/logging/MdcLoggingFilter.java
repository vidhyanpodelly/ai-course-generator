package com.aicoursegenerator.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_REQUEST_URI = "requestUri";
    private static final String MDC_METHOD = "method";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Get or generate Trace ID
            String traceId = request.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString();
            }
            MDC.put(MDC_TRACE_ID, traceId);
            response.setHeader(TRACE_ID_HEADER, traceId);

            // Request details
            MDC.put(MDC_REQUEST_URI, request.getRequestURI());
            MDC.put(MDC_METHOD, request.getMethod());

            // Extract user ID if authenticated
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
                try {
                    // Assuming principal is CustomUserDetails or similar that has an ID. For generic, we just use getName().
                    MDC.put(MDC_USER_ID, authentication.getName());
                } catch (Exception e) {
                    MDC.put(MDC_USER_ID, "unknown");
                }
            } else {
                MDC.put(MDC_USER_ID, "anonymous");
            }

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
