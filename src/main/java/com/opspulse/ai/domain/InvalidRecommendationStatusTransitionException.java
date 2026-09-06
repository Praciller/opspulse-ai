package com.opspulse.ai.domain;

public class InvalidRecommendationStatusTransitionException extends RuntimeException {
    public InvalidRecommendationStatusTransitionException(RecommendationStatus from, RecommendationStatus to) {
        super("Cannot transition recommendation from " + from + " to " + to);
    }
}
