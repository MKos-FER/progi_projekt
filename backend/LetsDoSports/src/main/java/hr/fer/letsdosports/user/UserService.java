package hr.fer.letsdosports.user;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository users;
    private final JdbcClient jdbc;

    public UserService(UserRepository users, JdbcClient jdbc){
        this.users=users;
        this.jdbc=jdbc;
    }

    public List<UserResponse> search(Short cityId,
                                     Short sportId,
                                     Short skillLevelId,
                                     Short ageMin,
                                     Short ageMax){
        return users.search(cityId, sportId, skillLevelId,ageMin,ageMax);
    }

}
