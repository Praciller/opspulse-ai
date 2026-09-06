package com.opspulse.identity.infrastructure.security;

import com.opspulse.shared.config.OpenApiConfig;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IdentityOpenApiConfiguration {

    @Bean
    OpenApiCustomizer registrationSecurityCustomizer(IdentitySecurityProperties properties) {
        return openApi -> {
            var path = openApi.getPaths().get("/api/auth/register");
            if (path == null || path.getPost() == null) {
                return;
            }
            if (properties.registrationMode()
                    == IdentitySecurityProperties.RegistrationMode.ADMIN_ONLY) {
                path.getPost()
                        .setSecurity(List.of(new SecurityRequirement()
                                .addList(OpenApiConfig.BEARER_JWT_SCHEME)));
            } else {
                path.getPost().setSecurity(List.of());
            }
        };
    }
}
