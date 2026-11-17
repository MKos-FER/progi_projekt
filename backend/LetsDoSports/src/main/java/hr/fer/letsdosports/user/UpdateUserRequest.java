package hr.fer.letsdosports.user;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import hr.fer.letsdosports.sport.SportDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UpdateUserRequest {
        @JsonSetter(nulls = Nulls.FAIL) @Size(min = 1) private String name;
        @JsonSetter(nulls = Nulls.FAIL) @Size(min = 1) private String username;
        @JsonSetter(nulls = Nulls.FAIL) @Pattern(regexp = "muško|žensko|drugo", message = "Spol mora biti 'muško', 'žensko' ili 'drugo'") private String gender;
        @JsonSetter(nulls = Nulls.FAIL) @PastOrPresent private LocalDate birthDate;
        @JsonSetter(nulls = Nulls.FAIL) @Positive @Max(32767) private Short cityId;
        @JsonSetter(nulls = Nulls.FAIL) @Size(min = 1) private List<@Valid SportDto> sports;
}