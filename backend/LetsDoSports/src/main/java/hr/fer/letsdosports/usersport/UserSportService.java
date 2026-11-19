package hr.fer.letsdosports.usersport;

import hr.fer.letsdosports.event.ApiValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

// Sportovi korisnika s razinom iskustva (F-004)
@Service
public class UserSportService {
    private final JdbcClient jdbc;

    public UserSportService(JdbcClient jdbc) { this.jdbc = jdbc; }

    // Zamjenjuje cijeli popis sportova korisnika. Pridružuje se transakciji pozivatelja
    // (PATCH /users/me), pa se profil i sportovi spremaju zajedno ili nikako.
    // Baza (V7) pri COMMIT-u dodatno odbija korisnika bez ijednog sporta.
    @Transactional
    public void replace(long userId, List<SportSelection> sports) {
        validate(sports);
        // Zaključavanje korisnika: dvije istodobne zamjene istog korisnika izvode se jedna za drugom
        boolean found = jdbc.sql("SELECT 1 FROM users WHERE id = :u FOR UPDATE")
                .param("u", userId).query(Integer.class).optional().isPresent();
        if (!found)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Korisnik " + userId + " ne postoji.");
        jdbc.sql("DELETE FROM user_sports WHERE user_id = :u").param("u", userId).update();
        for (SportSelection s : sports) {
            jdbc.sql("INSERT INTO user_sports (user_id, sport_id, skill_level_id) VALUES (:u, :s, :l)")
                    .param("u", userId).param("s", s.sportId()).param("l", s.skillLevelId())
                    .update();
        }
    }

    // Greške idu pod ključ "sports", kao i kod registracije
    public void validate(List<SportSelection> sports) {
        if (sports == null || sports.isEmpty())
            throw invalid("Potreban je barem jedan sport.");
        var seen = new HashSet<Short>();
        for (SportSelection s : sports) {
            if (s == null || s.sportId() == null || s.skillLevelId() == null)
                throw invalid("Svaki sport mora imati sport i razinu.");
            if (!seen.add(s.sportId()))
                throw invalid("Svaki sport smije biti naveden najviše jednom.");
            if (!exists("SELECT EXISTS(SELECT 1 FROM sports WHERE id = :v)", s.sportId()))
                throw invalid("Sport " + s.sportId() + " ne postoji.");
            if (!exists("SELECT EXISTS(SELECT 1 FROM sif_skill_level WHERE id = :v)", s.skillLevelId()))
                throw invalid("Razina " + s.skillLevelId() + " ne postoji.");
        }
    }

    private static ApiValidationException invalid(String message) {
        return new ApiValidationException(Map.of("sports", message));
    }

    private boolean exists(String sql, Object value) {
        return jdbc.sql(sql).param("v", value).query(Boolean.class).single();
    }
}
