package com.opspulse.ai.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(
        @NotBlank(message = "feedback is required")
        @Size(max = 2000, message = "feedback must be at most 2000 characters")
        String feedback) {}
