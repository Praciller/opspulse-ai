package com.opspulse.unit.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.opspulse.shared.security.CorsProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class CorsPropertiesTest {

    @Test
    void extraAllowedOriginsPreserveTheLegacyAllowlistAndRemoveDuplicates() {
        var properties = new CorsProperties(
                List.of(" https://opspulse-ai.pages.dev ", "https://opspulse-ai.pages.dev"),
                List.of(" https://opspulse-ai-gamma.vercel.app ", "", "https://opspulse-ai.pages.dev"));

        assertThat(properties.allAllowedOrigins())
                .containsExactly(
                        "https://opspulse-ai.pages.dev",
                        "https://opspulse-ai-gamma.vercel.app");
    }

    @Test
    void legacyConstructorRetainsExistingAllowlist() {
        var properties = new CorsProperties(List.of("https://opspulse-ai.pages.dev"));

        assertThat(properties.allAllowedOrigins()).containsExactly("https://opspulse-ai.pages.dev");
        assertThat(properties.extraAllowedOrigins()).isEmpty();
    }

    @Test
    void springBindsTheExtraOriginsWithoutReplacingTheOriginalOrigins() {
        new ApplicationContextRunner()
                .withUserConfiguration(CorsBindingConfig.class)
                .withPropertyValues(
                        "opspulse.cors.allowed-origins=https://opspulse-ai.pages.dev",
                        "opspulse.cors.extra-allowed-origins=https://opspulse-ai-gamma.vercel.app")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CorsProperties.class).allAllowedOrigins())
                            .containsExactly(
                                    "https://opspulse-ai.pages.dev",
                                    "https://opspulse-ai-gamma.vercel.app");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CorsProperties.class)
    static class CorsBindingConfig {}
}
