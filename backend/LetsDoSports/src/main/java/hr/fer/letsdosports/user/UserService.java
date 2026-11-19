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
import java.util.*;

@Service
public class UserService {

    private final UserRepository users;
    private final JdbcClient jdbc;

    public UserService(UserRepository users, JdbcClient jdbc){
        this.users=users;
        this.jdbc=jdbc;
    }

    public List<UserResponse> search(Short cityId, Short sportId, Short skillLevelId,
                                     Short ageMin, Short ageMax) {
        if (skillLevelId != null && sportId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "skillLevelId zahtijeva sportId.");
        }
        return users.search(cityId, sportId, skillLevelId, ageMin, ageMax);
    }

    public UserResponse getById(Long userId){
        return users.getById(userId);
    }

    public UserResponse getMine(Long userId){
        return users.getById(userId);
    }

    private boolean exists(String sql, Object... params) {
        return jdbc.sql(sql).params(List.of(params)).query(Boolean.class).single();
    }

    private void validateUser(Long userId, UpdateUserRequest req) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (req.getUsername() != null) {
            boolean taken = exists(
                    "SELECT EXISTS(SELECT 1 FROM users WHERE username = ? AND id <> ?)",
                    req.getUsername(), userId);
            if (taken) throw new ResponseStatusException(HttpStatus.CONFLICT, "Username zauzet");
        }

        if (req.getCityId() != null) {
            boolean cityOk = exists("SELECT EXISTS(SELECT 1 FROM sif_city WHERE id = ?)", req.getCityId());
            if (!cityOk) errors.put("cityId", "Grad ne postoji");
        }

        if (req.getSports() != null) {
            Set<Short> seen = new HashSet<>();
            for (SportDto s : req.getSports()) {
                if (!seen.add(s.sportId())) {
                    errors.put("sportId:" + s.sportId(), "Sport je naveden više puta.");
                }
            }
            for (SportDto sport : req.getSports()) {
                if (!exists("SELECT EXISTS(SELECT 1 FROM sports WHERE id = ?)", sport.sportId())) {
                    errors.put("sportId:" + sport.sportId(), "Sport " + sport.sportId() + " ne postoji.");
                }
                if (!exists("SELECT EXISTS(SELECT 1 FROM sif_skill_level WHERE id = ?)", sport.skillLevelId())) {
                    errors.put("skillLevelId:" + sport.skillLevelId(), "Skill level " + sport.skillLevelId() + " ne postoji.");
                }
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
    public void update(Long userId, UpdateUserRequest req){
        lockUser(userId);
        validateUser(userId, req);
        User u = users.getUserById(userId).orElseThrow();

        if(req.getName()!=null)u.setName(req.getName());
        if(req.getUsername()!=null)u.setUserName(req.getUsername());
        if(req.getGender()!=null)u.setGender(req.getGender());
        if(req.getBirthDate()!=null)u.setBirthDate(req.getBirthDate());
        if(req.getCityId()!=null)u.setCityId(req.getCityId());
        if(req.getSports()!=null)u.setSports(req.getSports());


        users.update(u);
    }
}
