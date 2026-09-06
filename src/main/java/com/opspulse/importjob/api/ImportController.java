package com.opspulse.importjob.api;

import com.opspulse.importjob.api.dto.ImportJobResponse;
import com.opspulse.importjob.api.dto.ImportRowErrorResponse;
import com.opspulse.importjob.application.port.in.ImportUseCase;
import com.opspulse.importjob.domain.ImportType;
import com.opspulse.shared.config.OpenApiConfig;
import com.opspulse.shared.observability.RequestIdContext;
import com.opspulse.shared.web.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping("/api/imports")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT_SCHEME)
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
public class ImportController {

    private final ImportUseCase imports;

    public ImportController(ImportUseCase imports) {
        this.imports = imports;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Start a CSV import")
    public ResponseEntity<ImportJobResponse> start(
            @RequestPart("file") MultipartFile file,
            @RequestParam String type,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication,
            HttpServletRequest request) throws java.io.IOException {
        final ImportType importType;
        try {
            importType = ImportType.parse(type);
        } catch (IllegalArgumentException exception) {
            throw new com.opspulse.shared.error.ApiException(
                    com.opspulse.shared.error.ErrorCode.IMPORT_TYPE_INVALID,
                    HttpStatus.BAD_REQUEST, "Unsupported import type");
        }
        var job = imports.start(new ImportUseCase.StartImportCommand(importType, idempotencyKey, file.getBytes(),
                UUID.fromString(authentication.getName()), RequestIdContext.currentRequestId(), request.getRemoteAddr()));
        return ResponseEntity.status(job.status() == com.opspulse.importjob.domain.ImportJobStatus.PENDING
                ? HttpStatus.ACCEPTED : HttpStatus.OK).body(ImportJobResponse.from(job));
    }

    @GetMapping
    public PagedResponse<ImportJobResponse> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = imports.findAll(page, size);
        return new PagedResponse<>(result.items().stream().map(ImportJobResponse::from).toList(), result.page(),
                result.size(), result.total(), result.totalPages());
    }

    @GetMapping("/{id}")
    public ImportJobResponse findById(@PathVariable UUID id) {
        return ImportJobResponse.from(imports.findById(id));
    }

    @GetMapping("/{id}/errors")
    public PagedResponse<ImportRowErrorResponse> findErrors(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = imports.findErrors(id, page, size);
        return new PagedResponse<>(result.items().stream().map(ImportRowErrorResponse::from).toList(), result.page(),
                result.size(), result.total(), result.totalPages());
    }
}
