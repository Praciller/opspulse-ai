package com.opspulse.shared.security;

import java.util.List;
import java.util.stream.Stream;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opspulse.cors")
public record CorsProperties(List<String> allowedOrigins, List<String> extraAllowedOrigins) {

    public CorsProperties {
        allowedOrigins = normalize(allowedOrigins);
        extraAllowedOrigins = normalize(extraAllowedOrigins);
    }

    public CorsProperties(List<String> allowedOrigins) {
        this(allowedOrigins, List.of());
    }

    public List<String> allAllowedOrigins() {
        return Stream.concat(allowedOrigins.stream(), extraAllowedOrigins.stream())
                .distinct()
                .toList();
    }

    private static List<String> normalize(List<String> origins) {
        return origins == null
                ? List.of()
                : origins.stream()
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .distinct()
                        .toList();
    }
}
