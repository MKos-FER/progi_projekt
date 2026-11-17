package hr.fer.letsdosports.event;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter @Setter
public class Event {
    private Long id;
    private Short sportId;
    private Long venueId;
    private Long createdById;
    private String name;
    private String description;
    private OffsetDateTime scheduledAt;
    private Integer expectedDurationMin;
    private Integer maxParticipants;
    private Short minAge;
    private Short maxAge;
    private Short minSkillLevelId;
    private OffsetDateTime cancelledAt;
    private OffsetDateTime createdAt;
}