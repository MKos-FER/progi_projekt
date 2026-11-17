package hr.fer.letsdosports.event;

import java.time.OffsetDateTime;

public record EventResponse(
        Long id, String name, String description, Short sportId, Long venueId,
        OffsetDateTime scheduledAt, Integer expectedDurationMin, Integer maxParticipants,
        Short minAge, Short maxAge, Short minSkillLevelId, OffsetDateTime createdAt,
        String status, Long organizerId, String organizerUsername, Long participantCount) {}