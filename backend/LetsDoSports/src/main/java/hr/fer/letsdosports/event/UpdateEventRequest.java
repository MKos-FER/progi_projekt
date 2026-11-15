package hr.fer.letsdosports.event;


import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;

public record UpdateEventRequest(
        @Pattern(regexp = ".*\\S.*", message = "Naziv ne smije biti prazan.")
        @Size(max = 40, message = "Naziv smije imati najviše 40 znakova.")
        String name,
        String description,
        Short sportId,
        Long venueId,
        @FutureOrPresent(message = "Termin mora biti sada ili u budućnosti.")
        OffsetDateTime scheduledAt,
        @Positive Integer expectedDurationMin,
        @Min(1) Integer maxParticipants,
        @Min(value = 13, message = "Dob ne smije biti ispod 13.") Short minAge,
        Short maxAge,
        Short minSkillLevelId
) {
    public UpdateEventRequest {
        if (name != null) name = name.trim();
    }
}
