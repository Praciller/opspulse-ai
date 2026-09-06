package com.opspulse;

import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class OpsPulseApplicationTests extends PostgresIntegrationTestSupport {

    @Test
    void contextLoads() {
    }
}
