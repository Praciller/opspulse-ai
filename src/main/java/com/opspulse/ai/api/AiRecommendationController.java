package com.opspulse.ai.api;

import com.opspulse.ai.api.dto.AiRecommendationResponse;
import com.opspulse.ai.api.dto.FeedbackRequest;
import com.opspulse.ai.api.dto.GenerateBriefRequest;
import com.opspulse.ai.application.port.in.BriefUseCase;
import com.opspulse.ai.domain.RecommendationStatus;
import com.opspulse.shared.config.OpenApiConfig;
import com.opspulse.shared.error.ApiException;
import com.opspulse.shared.error.ErrorCode;
import com.opspulse.shared.observability.RequestIdContext;
import com.opspulse.shared.web.PagedResponse;
import com.opspulse.shared.web.SortQuery;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/ai/recommendations")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT_SCHEME)
public class AiRecommendationController {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "status", "generatedBy", "modelProviderName");
    private final BriefUseCase briefs;

    public AiRecommendationController(BriefUseCase briefs) {
        this.briefs = briefs;
    }

    @GetMapping
    public PagedResponse<AiRecommendationResponse> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) RecommendationStatus status,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var sorting = SortQuery.parse(sort);
        if (!SORT_FIELDS.contains(sorting.field())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                    "Unsupported recommendation sort field");
        }
        var result = briefs.findAll(page, size, status, sorting.field(), sorting.ascending());
        return new PagedResponse<>(result.items().stream().map(AiRecommendationResponse::from).toList(),
                result.page(), result.size(), result.total(), result.totalPages());
    }

    @GetMapping("/{id}")
    public AiRecommendationResponse findById(@PathVariable UUID id) {
        return AiRecommendationResponse.from(briefs.findById(id));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public AiRecommendationResponse generate(
            @RequestBody(required = false) GenerateBriefRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        int topN = request == null || request.topN() == null ? 5 : request.topN();
        String promptVersion = request == null ? null : request.promptVersion();
        return AiRecommendationResponse.from(briefs.generate(topN, promptVersion, actor(authentication),
                RequestIdContext.currentRequestId(), servletRequest.getRemoteAddr()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public AiRecommendationResponse approve(
            @PathVariable UUID id, Authentication authentication, HttpServletRequest servletRequest) {
        return AiRecommendationResponse.from(briefs.approve(id, actor(authentication),
                RequestIdContext.currentRequestId(), servletRequest.getRemoteAddr()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public AiRecommendationResponse reject(
            @PathVariable UUID id, Authentication authentication, HttpServletRequest servletRequest) {
        return AiRecommendationResponse.from(briefs.reject(id, actor(authentication),
                RequestIdContext.currentRequestId(), servletRequest.getRemoteAddr()));
    }

    @PatchMapping("/{id}/feedback")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public AiRecommendationResponse feedback(
            @PathVariable UUID id,
            @Valid @RequestBody FeedbackRequest feedback,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        return AiRecommendationResponse.from(briefs.feedback(id, feedback.feedback(), actor(authentication),
                RequestIdContext.currentRequestId(), servletRequest.getRemoteAddr()));
    }

    private static UUID actor(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
