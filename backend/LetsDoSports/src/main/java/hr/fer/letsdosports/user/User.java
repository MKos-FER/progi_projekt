package hr.fer.letsdosports.user;

import hr.fer.letsdosports.sport.Sport;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;


@Getter
@Setter
public class User {
    private Long id;
    private String mame;
    private String userName;
    private String photoUrl;
    private String gender;
    private OffsetDateTime dob;
    private Short cityId;
    private String role;
    private List<Sport> sports;
}
