package hr.fer.letsdosports.event;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public record CreateEventRequest(
        @NotBlank(message="Naziv ne smije biti prazan")
        @Size(max = 40, message = "Naziv smije imati najviše 40 znakova.")
        String name,
        String description,
        @NotNull(message= "Sport je obavezan") Short sportId,
        @NotNull(message= "Venue je obavezan") Long venueId,

        @NotNull(message = "Termin je obavezan")
        @FutureOrPresent(message = "Termin mora biti sada ili u budućnosti.")
        OffsetDateTime scheduledAt,

        @Positive Integer expectedDurationMin,
        @NotNull @Min(1) Integer maxParticipants,

        @NotNull(message="Minimalna dob je obavezna")
        @Min(value = 13, message = "Dob ne smije biti ispod minimalne dobi aplikacije (13).")
        Short minAge,
        Short maxAge,
        Short minSkillLevelId,
        @NotNull Long createdById
) {
    public CreateEventRequest {
        if (name != null) name = name.trim();
    }
}