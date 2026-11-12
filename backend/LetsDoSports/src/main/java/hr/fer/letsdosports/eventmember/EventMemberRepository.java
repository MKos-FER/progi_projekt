package hr.fer.letsdosports.eventmember;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EventMemberRepository {
    private final JdbcClient jdbc;
    public EventMemberRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public boolean existsByEventIdAndUserId(Long eventId, Long userId) {
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM event_members WHERE event_id = :e AND user_id = :u)")
                .param("e", eventId).param("u", userId).query(Boolean.class).single();
    }

    public long countByEventId(Long eventId) {
        return jdbc.sql("SELECT count(*) FROM event_members WHERE event_id = :e")
                .param("e", eventId).query(Long.class).single();
    }

    public void insert(Long eventId, Long userId) {
        jdbc.sql("INSERT INTO event_members (event_id, user_id) VALUES (:e, :u)")
                .param("e", eventId).param("u", userId).update();
    }

    public int deleteByEventIdAndUserId(Long eventId, Long userId) {
        return jdbc.sql("DELETE FROM event_members WHERE event_id = :e AND user_id = :u")
                .param("e", eventId).param("u", userId).update();
    }

    public Optional<Long> findNextUserId(Long eventId) {
        return jdbc.sql("SELECT user_id FROM event_members WHERE event_id = :e ORDER BY joined_at, id LIMIT 1")
                .param("e", eventId).query(Long.class).optional();
    }

    public List<EventMemberResponse> listMembers(Long eventId) {
        return jdbc.sql("""
                SELECT u.id AS user_id, u.username AS username, u.name AS name,
                       m.joined_at AS joined_at, (u.id = e.created_by_id) AS organizer
                FROM event_members m
                JOIN users u ON u.id = m.user_id
                JOIN events e ON e.id = m.event_id
                WHERE m.event_id = :e
                ORDER BY m.joined_at, m.id""")
                .param("e", eventId).query(EventMemberResponse.class).list();
    }
}