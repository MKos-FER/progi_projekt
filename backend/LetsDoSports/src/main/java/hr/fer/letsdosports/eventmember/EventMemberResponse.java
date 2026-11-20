package hr.fer.letsdosports.eventmember;

import java.time.OffsetDateTime;
import java.util.List;

public record EventMemberResponse(
        Long userId,
        String username,
        String name,
        OffsetDateTime joinedAt,
        String profilePhotoUrl,
        List<MemberSport> sports,
        boolean organizer
) {
    public record MemberSport(Long sportId, Long skillLevelId) {}
}