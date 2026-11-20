package hr.fer.letsdosports.event;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class EventRepository {
    private final JdbcClient jdbc;
    public EventRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    private static final String RESPONSE_SELECT = """
        SELECT e.id, e.name, e.description, e.sport_id, e.venue_id, v.name as venue_name, v.latitude, v.longitude, e.scheduled_at,
               e.expected_duration_min, e.max_participants, e.min_age, e.max_age,
               e.min_skill_level_id, e.created_at, s.status,
               e.created_by_id AS organizer_id, u.username AS organizer_username,
               s.participant_count
        FROM events e
        JOIN events_with_status s ON s.id = e.id
        JOIN users u ON u.id = e.created_by_id
        JOIN venues v ON v.id = e.venue_id
        LEFT JOIN sif_city c ON c.id = v.city_id
        """;


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


    public List<EventResponse> search(Short[] sportIDs, Short[] cityIDs, OffsetDateTime fromTs,
                                      OffsetDateTime toTs, String status) {
        StringBuilder sql = new StringBuilder(RESPONSE_SELECT).append(" WHERE 1=1");
        Map<String, Object> p = new HashMap<>();
        if (cityIDs != null && cityIDs.length > 0) {
            sql.append(" AND ( ");
            for (int i = 0; i < cityIDs.length; i++) {
                Short cityId = cityIDs[i];
                if (i > 0) {
                    sql.append(" OR ");
                }
                sql.append("v.city_id = :cityId_" + cityId);
                p.put("cityId_" + cityId, cityId);
            }
            sql.append(" )");
        }
        if (sportIDs != null && sportIDs.length > 0) {
            sql.append(" AND ( ");
            for (int i = 0; i < sportIDs.length; i++) {
                Short sportId = sportIDs[i];
                if (i > 0) {
                    sql.append(" OR ");
                }
                sql.append("e.sport_id = :sportId_" + sportId);
                p.put("sportId_" + sportId, sportId);
            }
            sql.append(" )");
        }
        if (fromTs != null)  { sql.append(" AND e.scheduled_at >= :fromTs");        p.put("fromTs", fromTs); }
        if (toTs != null)    { sql.append(" AND e.scheduled_at <= :toTs");          p.put("toTs", toTs); }
        if (status != null)  { sql.append(" AND s.status = :status");               p.put("status", status); }
        sql.append(" ORDER BY e.scheduled_at");
        return jdbc.sql(sql.toString()).params(p).query(EventResponse.class).list();
    }

    public Optional<EventResponse> findDetailById(Long id) {
        return jdbc.sql(RESPONSE_SELECT + " WHERE e.id = :id")
                .param("id", id).query(EventResponse.class).optional();
    }

    public List<EventResponse> findJoinedBy(Long userId, String status) {
        return jdbc.sql(RESPONSE_SELECT + """
            WHERE e.id IN (SELECT event_id FROM event_members WHERE user_id = :userId)
              AND (CAST(:status AS text) IS NULL OR s.status = :status)
            ORDER BY e.scheduled_at""")
                .param("userId", userId).param("status", status)
                .query(EventResponse.class).list();
    }

    public Optional<Event> findById(Long id) {
        return jdbc.sql("SELECT * FROM events WHERE id = :id")
                .param("id", id).query(Event.class).optional();
    }
}