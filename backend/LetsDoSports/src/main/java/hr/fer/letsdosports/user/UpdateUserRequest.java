package hr.fer.letsdosports.user;

import hr.fer.letsdosports.sport.SportDto;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;

import java.time.OffsetDateTime;
import java.util.List;

public record UpdateUserRequest (
        String name,
        String username,
        @Pattern(regexp = "muško|žensko|drugo", message = "Spol mora biti 'muško', 'žensko' ili 'drugo'")String gender,
        @PastOrPresent(message="Datum rođenja mora biti u prošlosti")OffsetDateTime dob,
        Short cityId,
        List<SportDto> sports
        ){}
