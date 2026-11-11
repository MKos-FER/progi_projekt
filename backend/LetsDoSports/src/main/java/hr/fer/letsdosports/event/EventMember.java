package hr.fer.letsdosports.event;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "event_members")
@Getter @Setter
public class    EventMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "joined_at", insertable = false, updatable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "last_read_message_at")
    private OffsetDateTime lastReadMessageAt;
}