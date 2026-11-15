package hr.fer.letsdosports.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record RegisterRequest(
        @NotBlank(message = "Ime ne smije biti prazno.") String name,
        @NotBlank(message = "Korisničko ime ne smije biti prazno.") String username,
        @NotNull(message = "Spol je obavezan.")
        @Pattern(regexp = "Male|Female|Other", message = "Spol mora biti Male, Female ili Other.") String gender,
        @NotNull(message = "Datum rođenja je obavezan.")
        @PastOrPresent(message = "Datum rođenja ne smije biti u budućnosti.") LocalDate dateOfBirth,
        @NotNull(message = "Grad je obavezan.") @Positive Short cityId,
        @NotEmpty(message = "Potreban je barem jedan sport.") List<@Valid @NotNull SportSkill> sports) {

    public record SportSkill(
            @NotNull(message = "Sport je obavezan.") @Positive Short sportId,
            @NotNull(message = "Razina je obavezna.") @Positive Short skillLevelId) {}
}
