package com.omar.sentra.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Describes the internal payment-service API exposed through Springdoc.
 */
@Configuration
public class OpenApiConfiguration {

    /**
     * Creates the internal OpenAPI document metadata.
     *
     * @return OpenAPI metadata
     */
    @Bean
    OpenAPI paymentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sentra Payment Service Internal API")
                        .version("1.0.0")
                        .description("""
                                Internal payment-service API consumed by Sentra Gateway.
                                API-key validation, HMAC request-signature validation, and replay nonce checks are performed by the gateway.
                                Direct partner access to these internal paths is unsupported.
                                """))
                .servers(List.of(new Server()
                        .url("http://payment-service:8083")
                        .description("Internal service DNS name")));
    }
}
