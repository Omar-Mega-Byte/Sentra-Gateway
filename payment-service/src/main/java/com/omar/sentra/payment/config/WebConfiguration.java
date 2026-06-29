package com.omar.sentra.payment.config;

import com.omar.sentra.payment.common.request.TrustedContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers payment-service MVC infrastructure.
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    private final TrustedContextInterceptor trustedContextInterceptor;

    public WebConfiguration(TrustedContextInterceptor trustedContextInterceptor) {
        this.trustedContextInterceptor = trustedContextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(trustedContextInterceptor).addPathPatterns("/internal/v1/**");
    }
}
