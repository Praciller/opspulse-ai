package com.opspulse.ai.infrastructure.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
class SpringAiProviderConfiguration {

    @Bean
    @ConditionalOnExpression("'${AI_API_KEY:}' != ''")
    ChatClient springAiChatClient(
            @Value("${AI_API_KEY}") String apiKey,
            @Value("${AI_BASE_URL:https://api.openai.com}") String baseUrl,
            @Value("${opspulse.ai.model:gpt-4o-mini}") String model) {
        var api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory()))
                .build();
        var chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(model).temperature(0.0).build())
                .retryTemplate(RetryTemplate.builder().maxAttempts(1).build())
                .build();
        return ChatClient.create(chatModel);
    }

    private static JdkClientHttpRequestFactory requestFactory() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(15))
                .build());
        factory.setReadTimeout(Duration.ofSeconds(15));
        return factory;
    }
}
