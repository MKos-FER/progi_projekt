package hr.fer.letsdosports.event;

import java.time.OffsetDateTime;

public record EventMemberResponse(
        Long userId,
        String username,
        String name,
        OffsetDateTime joinedAt,
        boolean organizer
) {}