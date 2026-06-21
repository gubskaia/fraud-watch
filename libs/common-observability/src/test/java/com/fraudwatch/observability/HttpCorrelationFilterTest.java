package com.fraudwatch.observability;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HttpCorrelationFilterTest {

    private final HttpCorrelationFilter filter = new HttpCorrelationFilter();

    @Test
    void shouldReuseIncomingCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpCorrelationFilter.HEADER_NAME, "corr-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(HttpCorrelationFilter.HEADER_NAME)).isEqualTo("corr-123");
        assertThat(((HttpServletRequest) chain.getRequest()).getHeader(HttpCorrelationFilter.HEADER_NAME)).isEqualTo("corr-123");
        assertThat(MDC.get(HttpCorrelationFilter.MDC_KEY)).isNull();
    }

    @Test
    void shouldGenerateCorrelationIdWhenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(HttpCorrelationFilter.HEADER_NAME)).isNotBlank();
        assertThat(MDC.get(HttpCorrelationFilter.MDC_KEY)).isNull();
    }
}
