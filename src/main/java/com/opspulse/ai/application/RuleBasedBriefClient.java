package com.opspulse.ai.application;

import com.opspulse.ai.application.port.out.OperationsBriefClient;
import com.opspulse.ai.domain.BriefAction;
import com.opspulse.ai.domain.BriefRequest;
import com.opspulse.ai.domain.BriefResponse;
import com.opspulse.ai.domain.BriefRisk;
import com.opspulse.ai.domain.MessageDraft;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component("ruleBasedBriefClient")
public class RuleBasedBriefClient implements OperationsBriefClient {

    @Override
    public BriefResponse generate(BriefRequest request) {
        var actions = request.risks().stream()
                .map(risk -> new BriefAction(risk.id(), risk.recommendedAction(), risk.severity().name()))
                .toList();
        var drafts = new ArrayList<MessageDraft>();
        for (BriefRisk risk : request.risks()) {
            if (risk.entityType().name().equals("SUPPLIER")) {
                drafts.add(new MessageDraft("SUPPLIER_FOLLOWUP", null,
                        "Operations follow-up: " + risk.riskType().name(),
                        "Please review the supplier issue linked to risk " + risk.id() + ". Evidence: "
                                + risk.explanation()));
            } else if (risk.entityType().name().equals("ORDER")) {
                drafts.add(new MessageDraft("CUSTOMER_DELAY", null,
                        "Order operations update", "Please review the customer order associated with risk "
                                + risk.id() + ". Evidence: " + risk.explanation()));
            }
        }
        String summary = request.risks().isEmpty()
                ? "No open risk events require attention."
                : "Top " + request.risks().size() + " risks require attention: "
                        + request.risks().stream().map(risk -> risk.severity().name() + " " + risk.riskType().name())
                                .collect(java.util.stream.Collectors.joining(", ")) + ".";
        return BriefResponse.ruleBased(request.promptVersion(), summary, actions, drafts,
                request.risks().stream().map(BriefRisk::id).toList());
    }
}
