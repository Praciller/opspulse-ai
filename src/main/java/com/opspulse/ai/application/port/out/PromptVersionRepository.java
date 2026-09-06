package com.opspulse.ai.application.port.out;

import java.util.Optional;

public interface PromptVersionRepository {
    Optional<PromptVersion> findActive();
    Optional<PromptVersion> findByVersion(String version);

    record PromptVersion(String version, String template) {}
}
