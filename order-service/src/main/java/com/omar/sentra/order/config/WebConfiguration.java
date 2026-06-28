package com.omar.sentra.order.config;

import com.omar.sentra.order.common.request.TrustedContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers request-boundary authorization before controller argument parsing.
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    private final TrustedContextInterceptor trustedContextInterceptor;

    public WebConfiguration(TrustedContextInterceptor trustedContextInterceptor) {
        this.trustedContextInterceptor = trustedContextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(trustedContextInterceptor)
                .addPathPatterns(
                        "/internal/v1/orders",
                        "/internal/v1/orders/**",
                        "/internal/v1/admin/orders",
                        "/internal/v1/admin/orders/**");
    }
}
