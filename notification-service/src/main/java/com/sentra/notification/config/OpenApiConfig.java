package com.sentra.notification.config;

import com.sentra.notification.common.error.ApiError;
import com.sentra.notification.web.AdminTestRequest;
import com.sentra.notification.web.AdminTestResponse;
import com.sentra.notification.web.NotificationResponse;
import com.sentra.notification.web.PreferenceResponse;
import com.sentra.notification.web.PreferenceUpdateRequest;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures generated OpenAPI metadata for the internal notification service
 * contract.
 */
@Configuration
public class OpenApiConfig {
    /** @return OpenAPI customizer model */
    @Bean
    public OpenAPI notificationOpenApi() {
        Components components = new Components();
        ModelConverters converters = ModelConverters.getInstance();
        converters.read(ApiError.class).forEach(components::addSchemas);
        converters.read(NotificationResponse.class).forEach(components::addSchemas);
        converters.read(PreferenceUpdateRequest.class).forEach(components::addSchemas);
        converters.read(PreferenceResponse.class).forEach(components::addSchemas);
        converters.read(AdminTestRequest.class).forEach(components::addSchemas);
        converters.read(AdminTestResponse.class).forEach(components::addSchemas);
        return new OpenAPI()
                .info(new Info()
                        .title("Sentra Notification Service Internal API")
                        .version("1.0.0")
                        .description("""
                                Internal notification demonstration API for Sentra Gateway resilience verification.
                                External clients call the gateway routes; the gateway owns JWT validation, retry, timeout,
                                circuit-breaker, fallback, and route rewrite behavior. This service validates trusted
                                context headers, deterministic notification state, preferences, local/test fault controls,
                                errors, health, metrics, and redacted telemetry.
                                """)
                        .contact(new Contact().name("Sentra Gateway project")))
                .addServersItem(new Server().url("http://notification-service:8084").description("Internal service DNS and port used by gateway-service"))
                .components(components);
    }
}
