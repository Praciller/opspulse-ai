package com.opspulse.shared.config;

import com.opspulse.shared.error.ApiErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Phase2OpenApiConfiguration {

    private static final Set<String> IMPLEMENTED_PREFIXES = Set.of(
            "/api/products",
            "/api/suppliers",
            "/api/orders",
            "/api/purchase-orders",
            "/api/inventory-movements",
            "/api/risks",
            "/api/admin/outbox-events",
            "/api/ai/recommendations",
            "/api/imports",
            "/api/reports",
            "/api/dashboard");

    @Bean
    OpenApiCustomizer phase2ErrorResponses() {
        return openApi -> {
            ModelConverters.getInstance().read(ApiErrorResponse.class)
                    .forEach(openApi.getComponents()::addSchemas);
            openApi.getPaths().forEach((path, item) -> {
                if (IMPLEMENTED_PREFIXES.stream().noneMatch(path::startsWith)) {
                    return;
                }
                item.readOperationsMap().forEach((method, operation) -> {
                    ApiResponses responses = operation.getResponses();
                    normalizeSuccessResponse(path, method, responses);
                    addIfMissing(responses, "400", "Invalid request");
                    addIfMissing(responses, "401", "Authentication required");
                    addIfMissing(responses, "403", "Insufficient role");
                    if (path.contains("{")) {
                        addIfMissing(responses, "404", "Resource not found");
                    }
                if (method != io.swagger.v3.oas.models.PathItem.HttpMethod.GET) {
                    addIfMissing(responses, "409", "Uniqueness, state, or concurrency conflict");
                    }
                    if (path.equals("/api/inventory-movements")
                            && method == io.swagger.v3.oas.models.PathItem.HttpMethod.POST) {
                        addIfMissing(responses, "422", "Negative stock blocked");
                    }
                });
            });
        };
    }

    private static void normalizeSuccessResponse(
            String path,
            io.swagger.v3.oas.models.PathItem.HttpMethod method,
            ApiResponses responses) {
        if (method == io.swagger.v3.oas.models.PathItem.HttpMethod.POST
                && IMPLEMENTED_PREFIXES.contains(path)
                && responses.containsKey("200")) {
            ApiResponse created = responses.remove("200");
            created.setDescription("Created");
            responses.addApiResponse("201", created);
        }
        if (method == io.swagger.v3.oas.models.PathItem.HttpMethod.DELETE
                && responses.containsKey("200")) {
            responses.remove("200");
            responses.addApiResponse("204", new ApiResponse().description("Deactivated"));
        }
    }

    private static void addIfMissing(ApiResponses responses, String status, String description) {
        if (responses.containsKey(status)) {
            return;
        }
        var mediaType = new io.swagger.v3.oas.models.media.MediaType()
                .schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse"));
        responses.addApiResponse(status, new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json", mediaType)));
    }
}
