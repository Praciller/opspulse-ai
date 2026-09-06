package com.opspulse.integration.supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspulse.integration.support.Phase2ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class SupplierApiIntegrationTest extends Phase2ApiIntegrationTestSupport {

    @Test
    void supplierLifecycleIsPagedAuditedAndSoftDeleted() throws Exception {
        String operator = login("operator1@opspulse.demo");
        var created = mockMvc.perform(post("/api/suppliers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operator)
                        .header("X-Request-Id", "supplier-create-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Acme Supply",
                                  "contactInfo":{
                                    "email":"ops@acme.test",
                                    "phone":"+66-2000-0000",
                                    "address":"Bangkok"
                                  },
                                  "averageLeadTimeDays":3.50,
                                  "expectedSlaDays":5
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Acme Supply"))
                .andExpect(jsonPath("$.contactInfo.email").value("ops@acme.test"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        var json = objectMapper.readTree(created.getResponse().getContentAsString());
        String id = json.get("id").asText();

        mockMvc.perform(get("/api/suppliers")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + login("viewer@opspulse.demo"))
                        .param("search", "acme")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id))
                .andExpect(jsonPath("$.total").value(1));

        mockMvc.perform(put("/api/suppliers/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Acme Supply Updated",
                                  "contactInfo":{"email":"new@acme.test"},
                                  "averageLeadTimeDays":4,
                                  "expectedSlaDays":6,
                                  "version":0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme Supply Updated"))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(delete("/api/suppliers/{id}", id)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + login("admin@opspulse.demo"))
                        .param("version", "1"))
                .andExpect(status().isNoContent());

        Integer audits = jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where entity_id = ?::uuid and entity_type = 'SUPPLIER'",
                Integer.class,
                id);
        Integer outbox = jdbcTemplate.queryForObject(
                "select count(*) from outbox_events where aggregate_id = ?::uuid and event_type = 'opspulse.supplier.created'",
                Integer.class,
                id);
        assertThat(audits).isEqualTo(3);
        assertThat(outbox).isEqualTo(1);
    }
}
