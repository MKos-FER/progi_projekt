package hr.fer.letsdosports.user;

import hr.fer.letsdosports.sport.SportDto;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;
import java.util.List;

public record CreateUserRequest (
        @NotBlank(message="Ime ne smije biti prazno")String name,
        @NotBlank(message="Username ne smije biti prazan") String username,
        String photoUrl,
        @NotBlank(message="Spol ne smije biti prazan")@Pattern(regexp = "muško|žensko|drugo", message = "Spol mora biti 'muško', 'žensko' ili 'drugo'")String gender,
        @NotNull@PastOrPresent(message="Datum rođenja mora biti u prošlosti")
        OffsetDateTime dob,
        @NotNull Short appRoleId,
        @NotNull Short cityId,
        @NotNull@NotEmpty List<SportDto> sports
){
    public CreateUserRequest {
        if (name!=null)name=name.strip();
        if (username!=null)username=username.strip();
    }
}
