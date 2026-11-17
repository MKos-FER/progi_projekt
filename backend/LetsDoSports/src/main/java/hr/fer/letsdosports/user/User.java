package hr.fer.letsdosports.user;

import hr.fer.letsdosports.sport.SportDto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;


@Getter
@Setter
public class User {
    private Long id;
    private String name;
    private String userName;
    private String photoUrl;
    private String gender;
    private LocalDate birthDate;
    private Short cityId;
    private Short appRoleId;
    private List<SportDto> sports;
}
