package com.opspulse.ai.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspulse.ai.application.port.out.PromptVersionRepository.PromptVersion;
import com.opspulse.ai.domain.BriefRequest;
import com.opspulse.ai.domain.BriefRisk;
import java.util.LinkedHashMap;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {
    private final ObjectMapper objectMapper;

    public PromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String build(BriefRequest request, PromptVersion version) {
        var input = new LinkedHashMap<String, Object>();
        input.put("promptVersion", request.promptVersion());
        input.put("locale", request.locale().toLanguageTag());
        input.put("risks", request.risks().stream().map(this::riskMap).toList());
        try {
            return version.template() + "\n\nINPUT (JSON):\n" + objectMapper.writeValueAsString(input)
                    + "\n\nReturn JSON only. Do not mention unavailable facts.";
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to build AI prompt", exception);
        }
    }

    private LinkedHashMap<String, Object> riskMap(BriefRisk risk) {
        var map = new LinkedHashMap<String, Object>();
        map.put("riskEventId", risk.id().toString());
        map.put("riskType", risk.riskType().name());
        map.put("severity", risk.severity().name());
        map.put("entityType", risk.entityType().name());
        map.put("entityId", risk.entityId().toString());
        map.put("sourceMetrics", risk.sourceMetrics());
        map.put("explanation", risk.explanation());
        map.put("recommendedAction", risk.recommendedAction());
        map.put("createdAt", risk.createdAt().toString());
        return map;
    }
}
