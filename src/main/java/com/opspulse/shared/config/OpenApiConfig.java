package com.opspulse.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    public static final String BEARER_JWT_SCHEME = "bearer-jwt";

    @Bean
    OpenAPI opsPulseOpenApi(
            @Value("${info.app.version:0.0.1-SNAPSHOT}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title("OpsPulse-AI API")
                        .description("Operations intelligence API for small and medium enterprises")
                        .version(version))
                .components(new Components().addSecuritySchemes(
                        BEARER_JWT_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description(
                                        "HS256 access token returned by POST /api/auth/login "
                                                + "or POST /api/auth/refresh")));
    }
}
