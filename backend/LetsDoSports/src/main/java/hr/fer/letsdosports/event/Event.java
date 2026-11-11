package hr.fer.letsdosports.event;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "events")
@Getter @Setter
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sport_id", nullable = false)
    private Short sportId;

    @Column(name = "venue_id", nullable = false)
    private Long venueId;

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    private String name;
    private String description;

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(name = "expected_duration_min")
    private Integer expectedDurationMin;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    @Column(name = "min_age")
    private Short minAge;

    @Column(name = "max_age")
    private Short maxAge;

    @Column(name = "min_skill_level_id")
    private Short minSkillLevelId;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
