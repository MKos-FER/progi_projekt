package hr.fer.letsdosports.lookup;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LookupRepository {
    private final JdbcClient jdbc;

    public LookupRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<LookupItem> sports() {
        return jdbc.sql("SELECT id, name FROM sports ORDER BY name, id")
                .query(LookupItem.class).list();
    }

    public List<LookupItem> cities() {
        return jdbc.sql("SELECT id, name FROM sif_city ORDER BY name, id")
                .query(LookupItem.class).list();
    }

    public List<SkillLevelItem> skillLevels() {
        return jdbc.sql("SELECT id, name, rank FROM sif_skill_level ORDER BY rank")
                .query(SkillLevelItem.class).list();
    }
}
