package com.opspulse.importjob.application;

import com.opspulse.audit.application.port.in.RecordAuditEventUseCase;
import com.opspulse.audit.domain.AuditEvent;
import com.opspulse.importjob.application.port.in.ImportUseCase.ImportRowError;
import com.opspulse.importjob.application.port.out.ImportJobRepository;
import com.opspulse.importjob.domain.ImportJob;
import com.opspulse.importjob.domain.ImportJobStatus;
import com.opspulse.importjob.domain.ImportType;
import com.opspulse.inventory.application.port.in.InventoryUseCase;
import com.opspulse.inventory.domain.MovementType;
import com.opspulse.order.application.port.in.OrderUseCase;
import com.opspulse.product.application.port.in.ProductUseCase;
import com.opspulse.purchase.application.port.in.PurchaseOrderUseCase;
import com.opspulse.supplier.application.port.in.SupplierUseCase;
import com.opspulse.shared.error.ApiException;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ImportProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImportProcessor.class);
    private static final int MAX_ROWS = 10_000;
    private final ImportJobRepository jobs;
    private final ProductUseCase products;
    private final SupplierUseCase suppliers;
    private final OrderUseCase orders;
    private final InventoryUseCase inventory;
    private final PurchaseOrderUseCase purchaseOrders;
    private final RecordAuditEventUseCase audit;
    private final Clock clock;
    private final MeterRegistry metrics;

    public ImportProcessor(ImportJobRepository jobs, ProductUseCase products, SupplierUseCase suppliers,
                           OrderUseCase orders, InventoryUseCase inventory, PurchaseOrderUseCase purchaseOrders,
                           RecordAuditEventUseCase audit, Clock clock, MeterRegistry metrics) {
        this.jobs = jobs;
        this.products = products;
        this.suppliers = suppliers;
        this.orders = orders;
        this.inventory = inventory;
        this.purchaseOrders = purchaseOrders;
        this.audit = audit;
        this.clock = clock;
        this.metrics = metrics;
    }

    public void process(ImportJob job, byte[] content, String requestId, String ipAddress) {
        Instant started = clock.instant();
        jobs.updateStatus(job.id(), ImportJobStatus.PROCESSING, null, null, null, started, null);
        int total = 0;
        int successes = 0;
        int errors = 0;
        int bomOffset = hasUtf8Bom(content) ? 3 : 0;
        try (var reader = new InputStreamReader(
                     new ByteArrayInputStream(content, bomOffset, content.length - bomOffset), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true).setTrim(true).build().parse(reader)) {
            requireHeaders(job.importType(), parser.getHeaderNames());
            for (CSVRecord record : parser) {
                total++;
                if (total > MAX_ROWS) {
                    throw new ImportFatalException("IMPORT_ROW_LIMIT", "CSV file exceeds the 10,000 row limit");
                }
                try {
                    importRow(job, record, requestId, ipAddress);
                    successes++;
                } catch (RuntimeException exception) {
                    errors++;
                    jobs.addError(new ImportRowError(UUID.randomUUID(), job.id(), (int) record.getRecordNumber(),
                            record.toString(), errorCode(exception), safeMessage(exception), clock.instant()));
                }
            }
            Instant finished = clock.instant();
            jobs.updateStatus(job.id(), ImportJobStatus.COMPLETED, total, successes, errors, started, finished);
            metrics.counter("opspulse.import.completed", "type", job.importType().wireValue()).increment();
            metrics.counter("opspulse.import.rows", "result", "success").increment(successes);
            metrics.counter("opspulse.import.rows", "result", "error").increment(errors);
            audit.recordInNewTransaction(new AuditEvent(UUID.randomUUID(), job.createdBy(), "import.complete", "IMPORT_JOB",
                    job.id(), null, Map.of("status", ImportJobStatus.COMPLETED.name(), "totalRows", total,
                            "successRows", successes, "errorRows", errors), requestId, ipAddress, finished));
        } catch (ImportFatalException exception) {
            Instant finished = clock.instant();
            jobs.updateStatus(job.id(), ImportJobStatus.FAILED, total, successes, errors, started, finished);
            metrics.counter("opspulse.import.failed", "type", job.importType().wireValue()).increment();
            audit.recordInNewTransaction(new AuditEvent(UUID.randomUUID(), job.createdBy(), "import.failed", "IMPORT_JOB",
                    job.id(), null, Map.of("status", ImportJobStatus.FAILED.name(), "errorCode", exception.code()),
                    requestId, ipAddress, finished));
        } catch (Exception exception) {
            log.warn("Import job failed jobId={} errorCode=IMPORT_PROCESSING_FAILED", job.id());
            Instant finished = clock.instant();
            jobs.updateStatus(job.id(), ImportJobStatus.FAILED, total, successes, errors, started, finished);
            metrics.counter("opspulse.import.failed", "type", job.importType().wireValue()).increment();
            audit.recordInNewTransaction(new AuditEvent(UUID.randomUUID(), job.createdBy(), "import.failed", "IMPORT_JOB",
                    job.id(), null, Map.of("status", ImportJobStatus.FAILED.name(), "errorCode", "IMPORT_PROCESSING_FAILED"),
                    requestId, ipAddress, finished));
        }
    }

    private void importRow(ImportJob job, CSVRecord row, String requestId, String ipAddress) {
        switch (job.importType()) {
            case PRODUCTS -> products.create(new ProductUseCase.CreateProductCommand(
                    required(row, "sku"), required(row, "name"), optional(row, "category"),
                    defaulted(row, "unit", "PCS"), BigDecimal.ZERO, decimal(row, "safetyStock"),
                    decimal(row, "reorderPoint"), decimal(row, "cost"), decimal(row, "sellingPrice"),
                    job.createdBy(), requestId, ipAddress));
            case SUPPLIERS -> suppliers.create(new SupplierUseCase.SupplierCommand(
                    required(row, "name"), contact(row), optionalDecimal(row, "averageLeadTimeDays"),
                    integer(row, "expectedSlaDays"), 0, job.createdBy(), requestId, ipAddress));
            case ORDERS -> orders.create(new OrderUseCase.CreateOrderCommand(
                    required(row, "orderNumber"), required(row, "customerName"), date(row, "expectedShipDate"),
                    List.of(new OrderUseCase.ItemCommand(uuid(row, "productId"), decimal(row, "quantity"),
                            decimal(row, "unitPrice"))), job.createdBy(), requestId, ipAddress));
            case INVENTORY -> inventory.createMovement(new InventoryUseCase.CreateMovementCommand(
                    uuid(row, "productId"), enumValue(row, "movementType", MovementType.class), decimal(row, "quantity"),
                    required(row, "reason"), optional(row, "referenceType"), optionalUuid(row, "referenceId"),
                    job.createdBy(), requestId, ipAddress));
            case PO -> purchaseOrders.create(new PurchaseOrderUseCase.CreateCommand(
                    required(row, "poNumber"), uuid(row, "supplierId"), date(row, "expectedDeliveryDate"),
                    List.of(new PurchaseOrderUseCase.ItemCommand(uuid(row, "productId"), decimal(row, "quantity"),
                            decimal(row, "unitCost"))), job.createdBy(), requestId, ipAddress));
        }
    }

    private static void requireHeaders(ImportType type, List<String> headers) {
        List<String> required = switch (type) {
            case PRODUCTS -> List.of("sku", "name", "safetyStock", "reorderPoint", "cost", "sellingPrice");
            case SUPPLIERS -> List.of("name", "expectedSlaDays");
            case ORDERS -> List.of("orderNumber", "customerName", "expectedShipDate", "productId", "quantity", "unitPrice");
            case INVENTORY -> List.of("productId", "movementType", "quantity", "reason");
            case PO -> List.of("poNumber", "supplierId", "expectedDeliveryDate", "productId", "quantity", "unitCost");
        };
        if (!headers.containsAll(required)) {
            throw new ImportFatalException("IMPORT_HEADERS_INVALID", "CSV headers do not match the selected import type");
        }
    }

    private static String required(CSVRecord row, String key) {
        String value = optional(row, key);
        if (value == null) throw new IllegalArgumentException(key + " is required");
        return value;
    }

    private static String defaulted(CSVRecord row, String key, String fallback) {
        return optional(row, key) == null ? fallback : optional(row, key);
    }

    private static String optional(CSVRecord row, String key) {
        if (!row.isMapped(key)) return null;
        String value = row.get(key);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static UUID uuid(CSVRecord row, String key) { return UUID.fromString(required(row, key)); }
    private static UUID optionalUuid(CSVRecord row, String key) { String value = optional(row, key); return value == null ? null : UUID.fromString(value); }
    private static BigDecimal decimal(CSVRecord row, String key) { return new BigDecimal(required(row, key)); }
    private static BigDecimal optionalDecimal(CSVRecord row, String key) { String value = optional(row, key); return value == null ? null : new BigDecimal(value); }
    private static int integer(CSVRecord row, String key) { return Integer.parseInt(required(row, key)); }
    private static LocalDate date(CSVRecord row, String key) { return LocalDate.parse(required(row, key)); }
    private static <E extends Enum<E>> E enumValue(CSVRecord row, String key, Class<E> type) { return Enum.valueOf(type, required(row, key).toUpperCase(java.util.Locale.ROOT)); }

    private static Map<String, String> contact(CSVRecord row) {
        var values = new java.util.LinkedHashMap<String, String>();
        for (String key : List.of("email", "phone", "address")) {
            String value = optional(row, key);
            if (value != null) values.put(key, value);
        }
        return values;
    }

    private static String errorCode(RuntimeException exception) {
        if (exception instanceof ApiException api) return api.errorCode().name();
        if (exception instanceof NumberFormatException || exception instanceof java.time.format.DateTimeParseException
                || exception instanceof IllegalArgumentException) return "IMPORT_ROW_INVALID";
        return "IMPORT_ROW_FAILED";
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return "Row could not be imported";
        String sanitized = message.replaceAll("(?i)(password|token|secret|api[-_]?key)\\s*[:=]\\s*[^,; ]+", "$1=[REDACTED]")
                .replaceAll("[\\r\\n\\t]", " ");
        return sanitized.substring(0, Math.min(512, sanitized.length()));
    }

    private static boolean hasUtf8Bom(byte[] content) {
        return content.length >= 3
                && (content[0] & 0xFF) == 0xEF
                && (content[1] & 0xFF) == 0xBB
                && (content[2] & 0xFF) == 0xBF;
    }

    private static final class ImportFatalException extends RuntimeException {
        private final String code;

        private ImportFatalException(String code, String message) {
            super(message);
            this.code = code;
        }

        private String code() {
            return code;
        }
    }
}
