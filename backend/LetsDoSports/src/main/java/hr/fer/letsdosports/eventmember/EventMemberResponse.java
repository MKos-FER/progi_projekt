package hr.fer.letsdosports.eventmember;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

public record EventMemberResponse(
        Long userId,
        String username,
        String name,
        OffsetDateTime joinedAt,
        String photoUrl,
        List<MemberSport> sports,
        boolean organizer
) {
    public record MemberSport(Long sportId, Long skillLevelId) {}
}