package com.opspulse.ai.infrastructure.springai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspulse.ai.application.PromptBuilder;
import com.opspulse.ai.application.port.out.OperationsBriefClient;
import com.opspulse.ai.application.port.out.PromptVersionRepository;
import com.opspulse.ai.domain.AiClientException;
import com.opspulse.ai.domain.BriefAction;
import com.opspulse.ai.domain.BriefRequest;
import com.opspulse.ai.domain.BriefResponse;
import com.opspulse.ai.domain.GeneratedBy;
import com.opspulse.ai.domain.MessageDraft;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

@Component("springAiBriefClient")
public class SpringAiBriefClient implements OperationsBriefClient {

    private final ObjectProvider<ChatClient> clientProvider;
    private final PromptBuilder promptBuilder;
    private final PromptVersionRepository promptVersions;
    private final ObjectMapper objectMapper;
    private final String model;

    public SpringAiBriefClient(
            ObjectProvider<ChatClient> clientProvider,
            PromptBuilder promptBuilder,
            PromptVersionRepository promptVersions,
            ObjectMapper objectMapper,
            @Value("${opspulse.ai.model:gpt-4o-mini}") String model) {
        this.clientProvider = clientProvider;
        this.promptBuilder = promptBuilder;
        this.promptVersions = promptVersions;
        this.objectMapper = objectMapper;
        this.model = model;
    }

    @Override
    public BriefResponse generate(BriefRequest request) {
        var client = clientProvider.getIfAvailable();
        if (client == null) {
            throw new AiClientException("AI provider is not configured", "AI_NOT_CONFIGURED", false);
        }
        var version = promptVersions.findByVersion(request.promptVersion())
                .orElseThrow(() -> new AiClientException("AI prompt version is unavailable", "AI_PROMPT_VERSION", false));
        String prompt = promptBuilder.build(request, version);
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String content = client.prompt().user(prompt).call().content();
                if (content == null || content.isBlank()) {
                    throw new AiClientException("AI response was empty", "AI_EMPTY_RESPONSE", true);
                }
                return parse(content, request.promptVersion());
            } catch (AiClientException exception) {
                if (!exception.retryable() || attempt == 1) throw exception;
            } catch (Exception exception) {
                if (!isRetryable(exception) || attempt == 1) {
                    throw new AiClientException("AI provider request failed: "
                            + exception.getClass().getSimpleName(), "AI_PROVIDER_FAILURE", true, exception);
                }
            }
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AiClientException("AI provider request interrupted", "AI_INTERRUPTED", false);
            }
        }
        throw new AiClientException("AI provider request failed", "AI_PROVIDER_FAILURE", true);
    }

    private static boolean isRetryable(Exception exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof HttpStatusCodeException status && status.getStatusCode().is4xxClientError()) {
                return false;
            }
            String type = current.getClass().getSimpleName();
            String message = String.valueOf(current.getMessage());
            if (type.contains("HttpClientError") || type.contains("BadRequest")
                    || message.matches("(?s).*\\b(400|401|403|404|422)\\b.*")) {
                return false;
            }
        }
        return true;
    }

    private BriefResponse parse(String content, String promptVersion) {
        try {
            JsonNode root = objectMapper.readTree(content);
            var actions = new ArrayList<BriefAction>();
            for (JsonNode action : root.path("actions")) {
                actions.add(new BriefAction(
                        UUID.fromString(action.path("riskEventId").asText()),
                        action.path("action").asText(), action.path("priority").asText("MEDIUM")));
            }
            var drafts = new ArrayList<MessageDraft>();
            for (JsonNode draft : root.path("messageDrafts")) {
                drafts.add(new MessageDraft(draft.path("type").asText(),
                        nullableText(draft, "to"), nullableText(draft, "subject"), draft.path("body").asText()));
            }
            var ids = new ArrayList<UUID>();
            for (JsonNode id : root.path("riskEventIds")) ids.add(UUID.fromString(id.asText()));
            Double confidence = root.hasNonNull("confidence") ? root.get("confidence").asDouble() : null;
            return new BriefResponse(promptVersion, "openai/" + model, GeneratedBy.AI,
                    root.path("summary").asText(), actions, drafts, confidence, ids,
                    null, null);
        } catch (Exception exception) {
            throw new AiClientException("AI response could not be validated", "AI_INVALID_RESPONSE", false);
        }
    }

    private static String nullableText(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }
}
