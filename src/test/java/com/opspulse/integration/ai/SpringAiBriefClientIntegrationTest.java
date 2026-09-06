package com.opspulse.integration.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspulse.ai.application.PromptBuilder;
import com.opspulse.ai.application.port.out.PromptVersionRepository;
import com.opspulse.ai.domain.BriefRequest;
import com.opspulse.ai.domain.BriefRisk;
import com.opspulse.ai.domain.AiClientException;
import com.opspulse.ai.infrastructure.springai.SpringAiBriefClient;
import com.opspulse.risk.domain.RiskEntityType;
import com.opspulse.risk.domain.RiskSeverity;
import com.opspulse.risk.domain.RiskType;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

class SpringAiBriefClientIntegrationTest {

    @RegisterExtension
    static final WireMockExtension WIREMOCK = WireMockExtension.newInstance()
            .options(com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @Test
    void parsesOpenAiCompatibleResponse() throws Exception {
        var mapper = new ObjectMapper();
        var content = "{\"summary\":\"Review stock\",\"actions\":[{\"riskEventId\":\"30000000-0000-0000-0000-000000000001\",\"action\":\"Replenish\",\"priority\":\"HIGH\"}],\"messageDrafts\":[],\"confidence\":0.8,\"riskEventIds\":[\"30000000-0000-0000-0000-000000000001\"]}";
        WIREMOCK.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(mapper.writeValueAsString(Map.of(
                                "choices", List.of(Map.of("message", Map.of("role", "assistant", "content", content))))))));
        var response = client().generate(request());
        assertThat(response.summary()).isEqualTo("Review stock");
        assertThat(response.generatedBy()).isEqualTo(com.opspulse.ai.domain.GeneratedBy.AI);
        assertThat(response.riskEventIds()).containsExactly(UUID.fromString("30000000-0000-0000-0000-000000000001"));
        WIREMOCK.verify(1, postRequestedFor(urlEqualTo("/v1/chat/completions")));
    }

    @Test
    void retriesServerFailuresOnceButDoesNotRetryClientFailures() {
        WIREMOCK.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"message\":\"temporary\"}}")));
        assertThatThrownBy(() -> client().generate(request())).isInstanceOf(AiClientException.class);
        WIREMOCK.verify(2, postRequestedFor(urlEqualTo("/v1/chat/completions")));

        WIREMOCK.resetRequests();
        WIREMOCK.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse().withStatus(400).withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"message\":\"bad request\"}}")));
        assertThatThrownBy(() -> client().generate(request())).isInstanceOf(AiClientException.class);
        WIREMOCK.verify(1, postRequestedFor(urlEqualTo("/v1/chat/completions")));
    }

    private SpringAiBriefClient client() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        var api = OpenAiApi.builder().baseUrl(WIREMOCK.baseUrl()).apiKey("test-only-key")
                .restClientBuilder(RestClient.builder().requestFactory(factory)).build();
        var model = OpenAiChatModel.builder().openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model("gpt-4o-mini").build())
                .retryTemplate(RetryTemplate.builder().maxAttempts(1).build()).build();
        @SuppressWarnings("unchecked")
        ObjectProvider<ChatClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(ChatClient.create(model));
        var versions = mock(PromptVersionRepository.class);
        when(versions.findByVersion("1.0.0"))
                .thenReturn(java.util.Optional.of(new PromptVersionRepository.PromptVersion("1.0.0", "Use evidence.")));
        return new SpringAiBriefClient(provider, new PromptBuilder(new ObjectMapper()), versions,
                new ObjectMapper(), "gpt-4o-mini");
    }

    private BriefRequest request() {
        var risk = new BriefRisk(UUID.fromString("30000000-0000-0000-0000-000000000001"),
                RiskType.STOCKOUT_RISK, RiskSeverity.HIGH, RiskEntityType.PRODUCT,
                UUID.fromString("40000000-0000-0000-0000-000000000001"), Map.of("stock", 1),
                "Below stock", "Replenish", Instant.EPOCH);
        return new BriefRequest("1.0.0", List.of(risk), "default", Locale.ENGLISH);
    }
}
