package com.opspulse.unit.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.opspulse.outbox.infrastructure.CloudEventsEnvelopeBuilder;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CloudEventsEnvelopeBuilderTest {

    @Test
    void buildsCompleteCloudEventsEnvelope() {
        var clock = Clock.fixed(Instant.parse("2026-07-13T12:00:00Z"), ZoneOffset.UTC);
        var builder = new CloudEventsEnvelopeBuilder(clock);
        UUID eventId = UUID.fromString("30000000-0000-0000-0000-000000000001");
        UUID productId = UUID.fromString("20000000-0000-0000-0000-000000000001");

        Map<String, Object> envelope = builder.build(
                eventId,
                "PRODUCT",
                productId,
                "opspulse.product.stock.changed",
                Map.of("productId", productId.toString(), "delta", "-5.000"));

        assertThat(envelope)
                .containsEntry("specversion", "1.0")
                .containsEntry("id", eventId.toString())
                .containsEntry("source", "/opspulse/products")
                .containsEntry("type", "opspulse.product.stock.changed")
                .containsEntry("subject", "product/" + productId)
                .containsEntry("time", "2026-07-13T12:00:00Z")
                .containsEntry("datacontenttype", "application/json")
                .containsKey("data")
                .hasSize(8);
    }

    @ParameterizedTest
    @CsvSource({
        "PRODUCT,opspulse.product.created,/opspulse/products,product",
        "PRODUCT,opspulse.product.updated,/opspulse/products,product",
        "PRODUCT,opspulse.product.stock.changed,/opspulse/products,product",
        "ORDER,opspulse.order.created,/opspulse/orders,order",
        "ORDER,opspulse.order.status.changed,/opspulse/orders,order",
        "ORDER,opspulse.order.delayed,/opspulse/orders,order",
        "PURCHASE_ORDER,opspulse.po.sent,/opspulse/purchase-orders,purchase-order",
        "PURCHASE_ORDER,opspulse.po.received,/opspulse/purchase-orders,purchase-order",
        "SUPPLIER,opspulse.supplier.created,/opspulse/suppliers,supplier"
    })
    void mapsEveryPhase2CatalogEventToItsCloudEventsSource(
            String aggregateType, String eventType, String source, String subjectType) {
        var builder = new CloudEventsEnvelopeBuilder(
                Clock.fixed(Instant.parse("2026-07-13T12:00:00Z"), ZoneOffset.UTC));
        UUID aggregateId = UUID.randomUUID();

        var envelope = builder.build(UUID.randomUUID(), aggregateType, aggregateId, eventType, Map.of("id", aggregateId.toString()));

        assertThat(envelope)
                .containsEntry("source", source)
                .containsEntry("type", eventType)
                .containsEntry("subject", subjectType + "/" + aggregateId);
    }
}
