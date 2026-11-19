package hr.fer.letsdosports.user;

import hr.fer.letsdosports.sport.SportDto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;


@Getter @Setter
public class User {
    private Long id;
    private String name;
    private String username;
    private String gender;
    private LocalDate dateOfBirth;
    private Short cityId;
    private List<SportDto> sports;
}
