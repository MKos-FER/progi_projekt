package hr.fer.letsdosports.user;

import hr.fer.letsdosports.event.ApiValidationException;
import hr.fer.letsdosports.sport.Sport;
import hr.fer.letsdosports.sport.SportDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public UserResponse getById(Long userId){
        return users.getById(userId);
    }

    public UserResponse getMine(Long userId){
        return users.getById(userId);
    }

    private boolean exists(String sql, Object id) {
        return jdbc.sql(sql).param(id).query(Boolean.class).single();
    }


    private void validateUser(String username, Short city_id, List<SportDto> sports){
        Map<String, String> errors = new LinkedHashMap<>();

        boolean usernameOk=!exists("SELECT EXISTS(SELECT 1 FROM users u WHERE u.username LIKE ?)", username);
        if (!usernameOk){
            errors.put("username", "Username zautet");
        }

        boolean cityOk= exists("SELECT EXISTS (SELECT 1 FROM sif_city WHERE id=?)", city_id);
        if (!cityOk){
            errors.put("city_id", "Grad ne postoji");
        }

        for (SportDto sport : sports){
            boolean sportOk=exists("SELECT EXISTS(SELECT 1 FROM sports WHERE id = ?)",sport.sportId());
            if(!sportOk){
                String err= "sportName:"+sport.sportName();
                errors.put(err, "Sport "+sport.sportName()+" ne postoji.");
            }
            boolean skillLevelOk=exists("SELECT EXISTS(SELECT 1 FROM sif_skill_level WHERE id = ?)",sport.skillLevelId());
            if(!skillLevelOk){
                String err= "SkillLevel:"+sport.skillLevelName();
                errors.put(err, "Skill level "+sport.skillLevelName()+" ne postoji.");
            }
        }

        if (!errors.isEmpty()) throw new ApiValidationException(errors);
    }

    public void lockUser(Long userId){
        boolean foundUser = jdbc.sql("SELECT  id FROM  users WHERE id = :id FOR UPDATE ").param("id", userId).query(Long.class).optional().isPresent();
        if (!foundUser){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Korisnik "+userId+" ne postoji.");
        }
    }

    @Transactional
    public User update(Long userId, UpdateUserRequest req){
        lockUser(userId);
        User u = users.getUserById(userId).orElseThrow();

        if(req.getName()!=null)u.setName(req.getName());
        if(req.getUsername()!=null)u.setUserName(req.getUsername());
        if(req.getGender()!=null)u.setGender(req.getGender());
        if(req.getBirthDate()!=null)u.setBirthDate(req.getBirthDate());
        if(req.getCityId()!=null)u.setCityId(req.getCityId());
        if(req.getSports()!=null)u.setSports(req.getSports());

        if(req.getName()!=null ||req.getUsername()!=null ||req.getGender()!=null ||req.getBirthDate()!=null ||req.getCityId()!=null ||req.getSports()!=null){
            validateUser(u.getUserName(),u.getCityId(),u.getSports());
        }

        return users.update(u).orElseThrow();
    }
}
