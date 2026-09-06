package com.opspulse.integration.phase2;

import static org.assertj.core.api.Assertions.assertThat;

import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class Phase2MigrationIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsPhase2TablesFromAcleanDatabase() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                select table_name
                  from information_schema.tables
                 where table_schema = 'public'
                   and table_name in (
                       'products', 'suppliers', 'orders', 'order_items',
                       'inventory_movements', 'purchase_orders',
                       'purchase_order_items', 'outbox_events', 'risk_events',
                       'processed_events', 'app_config', 'prompt_versions',
                       'ai_recommendations', 'ai_recommendation_items', 'ai_usage_audit'
                   )
                 order by table_name
                """,
                String.class);

        assertThat(tables).containsExactly(
                "ai_recommendation_items",
                "ai_recommendations",
                "ai_usage_audit",
                "app_config",
                "inventory_movements",
                "order_items",
                "orders",
                "outbox_events",
                "processed_events",
                "products",
                "prompt_versions",
                "purchase_order_items",
                "purchase_orders",
                "risk_events",
                "suppliers");
    }

    @Test
    void flywayAppliesPhase2VersionsInOrderAndInventoryRowsAreImmutable() {
        assertThat(jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success order by installed_rank",
                String.class)).containsSubsequence("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");

        var productId = java.util.UUID.randomUUID();
        jdbcTemplate.update("""
                insert into products(id, sku, name, unit, current_stock, safety_stock, reorder_point, cost, selling_price, active)
                values (?::uuid, ?, 'Migration Product', 'PCS', 0, 0, 0, 1, 2, true)
                """, productId.toString(), "MIGRATION-" + productId.toString().toUpperCase());
        var movementId = java.util.UUID.randomUUID();
        jdbcTemplate.update("""
                insert into inventory_movements(id, product_id, movement_type, quantity, balance_before, balance_after, created_by)
                values (?::uuid, ?::uuid, 'INBOUND', 1, 0, 1, '10000000-0000-0000-0000-000000000003'::uuid)
                """, movementId.toString(), productId.toString());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbcTemplate.update(
                        "update inventory_movements set reason = 'changed' where id = ?::uuid", movementId.toString()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("inventory movements are immutable");
    }

    @Test
    void phase3MigrationsCreateRiskIndexesAndSeedTypedConfiguration() {
        assertThat(jdbcTemplate.queryForObject(
                "select value::text from app_config where key='slowMovingDays'", String.class))
                .isEqualTo("30");
        assertThat(jdbcTemplate.queryForObject(
                "select value::text from app_config where key='riskScanCron'", String.class))
                .isEqualTo("\"0 0 7 * * *\"");
        assertThat(jdbcTemplate.queryForList(
                "select indexname from pg_indexes where schemaname='public' and tablename='risk_events' order by indexname",
                String.class)).contains(
                "idx_risk_status_severity_created", "idx_risk_dedup_status", "idx_risk_entity", "uq_risk_active_dedup");
    }

    @Test
    void phase4MigrationSeedsPromptAndCreatesRecommendationIndexes() {
        assertThat(jdbcTemplate.queryForObject(
                "select active from prompt_versions where version='1.0.0'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForList(
                "select indexname from pg_indexes where schemaname='public' and tablename='ai_recommendations' order by indexname",
                String.class)).contains("idx_ai_recommendations_created", "idx_ai_recommendations_status_created");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.table_constraints where table_name='ai_recommendation_items' and constraint_type='FOREIGN KEY'",
                Integer.class)).isEqualTo(2);
    }
}
