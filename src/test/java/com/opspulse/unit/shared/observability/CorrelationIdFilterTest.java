package com.opspulse.unit.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.opspulse.shared.observability.CorrelationIdFilter;
import com.opspulse.shared.observability.RequestIdContext;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void propagatesSafeRequestIdToMdcAndResponse() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(RequestIdContext.HEADER_NAME, "request-123");
        var response = new MockHttpServletResponse();
        var requestIdInsideChain = new AtomicReference<String>();

        filter.doFilter(
                request,
                response,
                (servletRequest, servletResponse) ->
                        requestIdInsideChain.set(MDC.get(RequestIdContext.MDC_KEY)));

        assertThat(requestIdInsideChain).hasValue("request-123");
        assertThat(response.getHeader(RequestIdContext.HEADER_NAME)).isEqualTo("request-123");
        assertThat(MDC.get(RequestIdContext.MDC_KEY)).isNull();
    }

    @Test
    void generatesRequestIdWhenHeaderIsMissing() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var requestIdInsideChain = new AtomicReference<String>();

        filter.doFilter(
                request,
                response,
                (servletRequest, servletResponse) ->
                        requestIdInsideChain.set(MDC.get(RequestIdContext.MDC_KEY)));

        assertThat(requestIdInsideChain.get()).isNotBlank();
        assertThat(response.getHeader(RequestIdContext.HEADER_NAME))
                .isEqualTo(requestIdInsideChain.get());
        assertThat(MDC.get(RequestIdContext.MDC_KEY)).isNull();
    }

    @Test
    void replacesUnsafeRequestId() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(RequestIdContext.HEADER_NAME, "unsafe\nvalue");
        var response = new MockHttpServletResponse();
        var requestIdInsideChain = new AtomicReference<String>();

        filter.doFilter(
                request,
                response,
                (servletRequest, servletResponse) ->
                        requestIdInsideChain.set(MDC.get(RequestIdContext.MDC_KEY)));

        assertThat(requestIdInsideChain.get()).isNotEqualTo("unsafe\nvalue").matches("[a-f0-9-]{36}");
    }
}
