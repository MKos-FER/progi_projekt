package hr.fer.letsdosports.sport;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SportDto(
        @NotNull @Positive @Max(32767) Short sportId,
        String sportName,
        @NotNull @Positive @Max(32767) Short skillLevelId,
        String skillLevelName
) {}
