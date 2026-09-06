package com.opspulse.integration.phase6;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspulse.integration.support.Phase2ApiIntegrationTestSupport;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ImportApiIntegrationTest extends Phase2ApiIntegrationTestSupport {

    @Test
    void operatorCanStartIdempotentImportAndReadErrors() throws Exception {
        String token = login("operator1@opspulse.demo");
        String key = "phase6-" + UUID.randomUUID();
        String sku = "IMPORT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var file = new MockMultipartFile("file", "products.csv", "text/csv",
                ("sku,name,safetyStock,reorderPoint,cost,sellingPrice\n" + sku + ",Imported,2,3,1,2\n").getBytes(StandardCharsets.UTF_8));
        var result = mockMvc.perform(multipart("/api/imports")
                        .file(file).param("type", "products")
                        .header("Idempotency-Key", key)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        String id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
        String status = "PENDING";
        for (int attempt = 0; attempt < 20 && !"COMPLETED".equals(status); attempt++) {
            Thread.sleep(100);
            var response = mockMvc.perform(get("/api/imports/" + id)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andReturn();
            status = objectMapper.readTree(response.getResponse().getContentAsString()).get("status").asText();
        }
        assertThat(status).isEqualTo("COMPLETED");
        var replay = mockMvc.perform(multipart("/api/imports")
                        .file(file).param("type", "products")
                        .header("Idempotency-Key", key)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        assertThat(objectMapper.readTree(replay.getResponse().getContentAsString()).get("id").asText()).isEqualTo(id);
    }

    @Test
    void viewerCannotStartImport() throws Exception {
        String token = login("viewer@opspulse.demo");
        var file = new MockMultipartFile("file", "products.csv", "text/csv", "sku,name\nX,Nope".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/imports").file(file).param("type", "products")
                        .header("Idempotency-Key", "viewer-" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
