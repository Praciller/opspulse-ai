package com.opspulse.ai.infrastructure.persistence.jdbc;

import com.opspulse.ai.application.port.out.PromptVersionRepository;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcPromptVersionRepository implements PromptVersionRepository {
    private final JdbcTemplate jdbc;

    JdbcPromptVersionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<PromptVersion> findActive() {
        return jdbc.query("select version, template from prompt_versions where active order by created_at desc limit 1",
                rs -> rs.next() ? Optional.of(new PromptVersion(rs.getString("version"), rs.getString("template"))) : Optional.empty());
    }

    @Override
    public Optional<PromptVersion> findByVersion(String version) {
        return jdbc.query("select version, template from prompt_versions where version = ?",
                rs -> rs.next() ? Optional.of(new PromptVersion(rs.getString("version"), rs.getString("template"))) : Optional.empty(), version);
    }
}
