package com.omar.sentra.user.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Describes the internal user-service contract published through springdoc.
 */
@Configuration
public class OpenApiConfiguration {

    /**
     * Creates the service OpenAPI metadata.
     *
     * @return OpenAPI model
     */
    @Bean
    OpenAPI userServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sentra User Service Internal API")
                        .version("1.0.0")
                        .description("""
                                Internal profile API consumed by Sentra Gateway. External JWT
                                authentication and route authorization are enforced by the gateway.
                                This service performs defense-in-depth validation of trusted context
                                headers and never accepts bearer tokens as downstream identity.
                                """)
                        .contact(new Contact().name("Sentra Platform Engineering")))
                .servers(List.of(new Server()
                        .url("http://user-service:8081")
                        .description("Internal service network")))
                .components(new Components());
    }
}
