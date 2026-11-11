package hr.fer.letsdosports.event;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository events;
    private final EventMemberRepository members;
    private final JdbcClient jdbc;

    public EventService(EventRepository events, EventMemberRepository members, JdbcClient jdbc) {
        this.events = events;
        this.members = members;
        this.jdbc = jdbc;
    }

    public List<Event> findAll() {
        return events.findAll();
    }

    public Optional<Event> findById(Long id) {
        return events.findById(id);
    }

    @Transactional
    public Event create(CreateEventRequest req) {
        requireUser(req.createdById());
        validateReferences(req.sportId(), req.venueId(), req.minAge(), req.maxAge(), req.minSkillLevelId());

        Event e = new Event();
        e.setName(req.name());
        e.setDescription(req.description());
        e.setSportId(req.sportId());
        e.setVenueId(req.venueId());
        e.setCreatedById(req.createdById());
        e.setScheduledAt(req.scheduledAt());
        e.setExpectedDurationMin(req.expectedDurationMin());
        e.setMaxParticipants(req.maxParticipants());
        e.setMinAge(req.minAge());
        e.setMaxAge(req.maxAge());
        e.setMinSkillLevelId(req.minSkillLevelId());
        e = events.save(e);

        EventMember organizer = new EventMember();
        organizer.setEventId(e.getId());
        organizer.setUserId(req.createdById());
        members.save(organizer);

        return e;
    }

    private void validateReferences(Short sportId, Long venueId, Short minAge, Short maxAge, Short minSkillLevelId) {
        Map<String, String> errors = new LinkedHashMap<>();

        boolean sportOk = exists("SELECT EXISTS(SELECT 1 FROM sports WHERE id = ?)", sportId);
        if (!sportOk) errors.put("sportId", "Sport ne postoji.");

        boolean venueOk = exists("SELECT EXISTS(SELECT 1 FROM venues WHERE id = ?)", venueId);
        if (!venueOk) {
            errors.put("venueId", "Venue ne postoji.");
        } else if (sportOk && !venueSupportsSport(venueId, sportId)) {
            errors.put("venueId", "Venue ne podržava odabrani sport.");
        }

        if (minAge != null && maxAge != null && minAge > maxAge) {
            errors.put("minAge", "Minimalna dob ne smije biti veća od maksimalne.");
        }

        if (minSkillLevelId != null
                && !exists("SELECT EXISTS(SELECT 1 FROM sif_skill_level WHERE id = ?)", minSkillLevelId)) {
            errors.put("minSkillLevelId", "Razina ne postoji.");
        }

        if (!errors.isEmpty()) throw new ApiValidationException(errors);
    }

    private boolean venueSupportsSport(long venueId, long sportId) {
        return true;
/*        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM venue_sports WHERE venue_id = ? AND sport_id = ?)")
                .param(venueId).param(sportId)
                .query(Boolean.class).single();*/
    }

    private boolean exists(String sql, Object id) {
        return jdbc.sql(sql).param(id).query(Boolean.class).single();
    }

    public List<EventMemberResponse> listMembers(Long eventId) {
        if (!events.existsById(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event " + eventId + " not found.");
        }

        return jdbc.sql("""
            SELECT u.id          AS user_id,
                   u.username    AS username,
                   u.name        AS name,
                   m.joined_at   AS joined_at,
                   (u.id = e.created_by_id) AS organizer
            FROM event_members m
            JOIN users  u ON u.id = m.user_id
            JOIN events e ON e.id = m.event_id
            WHERE m.event_id = ?
            ORDER BY m.joined_at, m.id
            """)
                .param(eventId)
                .query(EventMemberResponse.class)
                .list();
    }

    private void lockEvent(Long eventId) {
        boolean found = jdbc.sql("SELECT id FROM events WHERE id = :id FOR UPDATE")
                .param("id", eventId).query(Long.class).optional().isPresent();
        if (!found) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Događaj " + eventId + " ne postoji.");
        }
    }

    private String statusOf(Long eventId) {
        return jdbc.sql("SELECT status FROM events_with_status WHERE id = :id")
                .param("id", eventId).query(String.class).single();
    }

    private void requireUser(Long userId) {
        if (!exists("SELECT EXISTS(SELECT 1 FROM users WHERE id = ?)", userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Korisnik ne postoji.");
        }
    }

    private ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }


    public List<Event> findJoinedBy(Long userId) { return events.findJoinedBy(userId); }

    @Transactional
    public void cancel(Long eventId) {
        lockEvent(eventId);
        String status = statusOf(eventId);
        if (status.equals("cancelled")) throw conflict("Događaj je već otkazan.");
        if (status.equals("finished"))  throw conflict("Događaj je već završio.");
        jdbc.sql("UPDATE events SET cancelled_at = now() WHERE id = :id").param("id", eventId).update();
    }


    @Transactional
    public Event update(Long eventId, UpdateEventRequest req, Long requiredOwnerId) {
        lockEvent(eventId);
        Event e = events.findById(eventId).orElseThrow();

        if (requiredOwnerId != null && !requiredOwnerId.equals(e.getCreatedById())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Samo organizator može uređivati događaj.");
        }
        String status = statusOf(eventId);
        if (status.equals("cancelled") || status.equals("finished")) {
            throw conflict("Otkazani ili završeni događaj se ne može uređivati.");
        }

        if (req.name() != null)                e.setName(req.name());
        if (req.description() != null)         e.setDescription(req.description());
        if (req.sportId() != null)             e.setSportId(req.sportId());
        if (req.venueId() != null)             e.setVenueId(req.venueId());
        if (req.scheduledAt() != null)         e.setScheduledAt(req.scheduledAt());
        if (req.expectedDurationMin() != null) e.setExpectedDurationMin(req.expectedDurationMin());
        if (req.maxParticipants() != null)     e.setMaxParticipants(req.maxParticipants());
        if (req.minAge() != null)              e.setMinAge(req.minAge());
        if (req.maxAge() != null)              e.setMaxAge(req.maxAge());
        if (req.minSkillLevelId() != null)     e.setMinSkillLevelId(req.minSkillLevelId());

        if (req.sportId() != null || req.venueId() != null || req.minAge() != null
                || req.maxAge() != null || req.minSkillLevelId() != null) {
            validateReferences(e.getSportId(), e.getVenueId(), e.getMinAge(), e.getMaxAge(), e.getMinSkillLevelId());
        }
        if (req.maxParticipants() != null && req.maxParticipants() < members.countByEventId(eventId)) {
            throw new ApiValidationException(Map.of("maxParticipants", "Kapacitet ne može biti manji od broja sudionika."));
        }
        return events.save(e);
    }



    @Transactional
    public void join(Long eventId, Long userId) {
        lockEvent(eventId);
        requireUser(userId);

        if (members.existsByEventIdAndUserId(eventId, userId)) throw conflict("Već si prijavljen na ovaj događaj.");

        String status = statusOf(eventId);
        switch (status) {
            case "open" -> { }
            case "full" -> throw conflict("Događaj je popunjen.");
            case "cancelled" -> throw conflict("Događaj je otkazan.");
            default -> throw conflict("Događaj je već počeo ili završio.");
        }

        EventMember m = new EventMember();
        m.setEventId(eventId);
        m.setUserId(userId);
        members.save(m);
    }


    @Transactional
    public void removeMember(Long eventId, Long userId) {
        lockEvent(eventId);

        if (!statusOf(eventId).matches("open|full")) {
            throw conflict("Događaj je već počeo, završio ili je otkazan.");
        }

        long deleted = members.deleteByEventIdAndUserId(eventId, userId);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Korisnik nije član događaja.");
        }
        members.flush();

        Long ownerId = jdbc.sql("SELECT created_by_id FROM events WHERE id = :id")
                .param("id", eventId).query(Long.class).single();
        if (ownerId.equals(userId)) {
            Optional<Long> next = jdbc.sql(
                            "SELECT user_id FROM event_members WHERE event_id = :id ORDER BY joined_at, id LIMIT 1")
                    .param("id", eventId).query(Long.class).optional();
            if (next.isPresent()) {
                jdbc.sql("UPDATE events SET created_by_id = :u WHERE id = :id")
                        .param("u", next.get()).param("id", eventId).update();
            } else {
                jdbc.sql("UPDATE events SET cancelled_at = now() WHERE id = :id").param("id", eventId).update();
            }
        }
    }
}