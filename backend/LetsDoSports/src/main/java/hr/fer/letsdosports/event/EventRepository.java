package hr.fer.letsdosports.event;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EventRepository {
    private final JdbcClient jdbc;
    public EventRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<Event> findAll() {
        return jdbc.sql("SELECT * FROM events ORDER BY id").query(Event.class).list();
    }

    public Optional<Event> findById(Long id) {
        return jdbc.sql("SELECT * FROM events WHERE id = :id")
                .param("id", id).query(Event.class).optional();
    }

    public List<Event> findJoinedBy(Long userId) {
        return jdbc.sql("""
                SELECT e.* FROM events e
                WHERE e.id IN (SELECT event_id FROM event_members WHERE user_id = :userId)
                ORDER BY e.scheduled_at""")
                .param("userId", userId).query(Event.class).list();
    }

    public Event insert(Event e) {
        return jdbc.sql("""
                INSERT INTO events (name, description, sport_id, venue_id, created_by_id,
                    scheduled_at, expected_duration_min, max_participants,
                    min_age, max_age, min_skill_level_id)
                VALUES (:name, :description, :sportId, :venueId, :createdById,
                    :scheduledAt, :expectedDurationMin, :maxParticipants,
                    :minAge, :maxAge, :minSkillLevelId)
                RETURNING *""")
                .paramSource(e).query(Event.class).single();
    }

    public Event update(Event e) {
        return jdbc.sql("""
                UPDATE events SET name = :name, description = :description,
                    sport_id = :sportId, venue_id = :venueId, scheduled_at = :scheduledAt,
                    expected_duration_min = :expectedDurationMin,
                    max_participants = :maxParticipants, min_age = :minAge,
                    max_age = :maxAge, min_skill_level_id = :minSkillLevelId
                WHERE id = :id
                RETURNING *""")
                .paramSource(e).query(Event.class).single();
    }

    public void cancel(Long id) {
        jdbc.sql("UPDATE events SET cancelled_at = now() WHERE id = :id").param("id", id).update();
    }

    public Long findCreatedById(Long id) {
        return jdbc.sql("SELECT created_by_id FROM events WHERE id = :id")
                .param("id", id).query(Long.class).single();
    }

    public void setCreatedBy(Long id, Long userId) {
        jdbc.sql("UPDATE events SET created_by_id = :u WHERE id = :id")
                .param("u", userId).param("id", id).update();
    }
}