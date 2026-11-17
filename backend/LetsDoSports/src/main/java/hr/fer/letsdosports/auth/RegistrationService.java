package hr.fer.letsdosports.auth;

import hr.fer.letsdosports.event.ApiValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class RegistrationService {
    private final JdbcClient jdbc;

    public RegistrationService(JdbcClient jdbc) { this.jdbc = jdbc; }

    // ID korisnika povezanog s Google računom, ako je registracija dovršena.
    public Optional<Long> findLinkedUserId(OAuth2AuthenticationToken token) {
        return jdbc.sql("""
                        SELECT user_id FROM oauth_accounts
                        WHERE provider = :provider AND provider_user_id = :subject""")
                .param("provider", token.getAuthorizedClientRegistrationId())
                .param("subject", token.getName())
                .query(Long.class).optional();
    }

    // Stvara korisnika, njegove sportove i vezu na OAuth račun
    @Transactional
    public long register(OAuth2AuthenticationToken token, RegisterRequest req) {
        if (findLinkedUserId(token).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registracija je već dovršena.");
        validateReferences(req);
        if (exists("SELECT EXISTS(SELECT 1 FROM users WHERE username = :v)", req.username().trim()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Korisničko ime je zauzeto.");

        long userId = jdbc.sql("""
                        INSERT INTO users (username, name, gender, date_of_birth, city_id)
                        VALUES (:username, :name, :gender, :birthDate, :cityId)
                        RETURNING id""")
                .param("username", req.username().trim())
                .param("name", req.name().trim())
                .param("gender", req.gender())
                .param("birthDate", req.dateOfBirth())
                .param("cityId", req.cityId())
                .query(Long.class).single();

        for (RegisterRequest.SportSkill s : req.sports()) {
            jdbc.sql("INSERT INTO user_sports (user_id, sport_id, skill_level_id) VALUES (:u, :s, :l)")
                    .param("u", userId).param("s", s.sportId()).param("l", s.skillLevelId())
                    .update();
        }

        jdbc.sql("""
                        INSERT INTO oauth_accounts (user_id, provider, provider_user_id, email)
                        VALUES (:userId, :provider, :subject, :email)""")
                .param("userId", userId)
                .param("provider", token.getAuthorizedClientRegistrationId())
                .param("subject", token.getName())
                .param("email", token.getPrincipal().getAttribute("email"))
                .update();
        return userId;
    }

    private void validateReferences(RegisterRequest req) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!exists("SELECT EXISTS(SELECT 1 FROM sif_city WHERE id = :v)", req.cityId()))
            errors.put("cityId", "Grad ne postoji.");
        var seen = new HashSet<Short>();
        for (RegisterRequest.SportSkill s : req.sports()) {
            if (!seen.add(s.sportId()))
                errors.put("sports", "Svaki sport smije biti naveden najviše jednom.");
            else if (!exists("SELECT EXISTS(SELECT 1 FROM sports WHERE id = :v)", s.sportId()))
                errors.put("sports", "Sport " + s.sportId() + " ne postoji.");
            else if (!exists("SELECT EXISTS(SELECT 1 FROM sif_skill_level WHERE id = :v)", s.skillLevelId()))
                errors.put("sports", "Razina " + s.skillLevelId() + " ne postoji.");
        }
        if (!errors.isEmpty()) throw new ApiValidationException(errors);
    }

    private boolean exists(String sql, Object value) {
        return jdbc.sql(sql).param("v", value).query(Boolean.class).single();
    }
}
