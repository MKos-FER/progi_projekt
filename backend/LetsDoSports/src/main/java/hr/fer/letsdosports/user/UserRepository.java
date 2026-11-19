package hr.fer.letsdosports.user;

import hr.fer.letsdosports.sport.Sport;
import hr.fer.letsdosports.sport.SportDto;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.*;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

@Repository
public class UserRepository {
    private final JdbcClient jdbc;
    private final JsonMapper mapper;
    public UserRepository(JdbcClient jdbc, JsonMapper mapper) {
        this.jdbc = jdbc;this.mapper=mapper;
    }

    private UserResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        List<SportDto> sports = mapper.readValue(
                rs.getString("sports"), new TypeReference<List<SportDto>>() {});
        return new UserResponse(
                rs.getLong("user_id"),
                rs.getString("name"),
                rs.getString("username"),
                rs.getString("profile_photo_url"),
                rs.getString("gender"),
                rs.getObject("date_of_birth", LocalDate.class),
                rs.getObject("age", Integer.class),
                rs.getObject("city_id", Short.class),
                rs.getString("app_role_id"),
                sports,
                rs.getObject("created_at", OffsetDateTime.class));
    }

    private static final String RESPONSE_SELECT = """
        SELECT u.id as user_id,
               u.name,
               u.username,
               u.profile_photo_url,
               u.gender,
               u.date_of_birth,
               EXTRACT(YEAR FROM age(u.date_of_birth))::int AS age,
               u.city_id,
               u.app_role_id,
               COALESCE(s.sports, '[]'::json) AS sports,
               u.created_at
        FROM users u
        LEFT JOIN LATERAL (
            SELECT json_agg(json_build_object(
                       'sportId', sp.id,
                       'sportName', sp.name,
                       'skillLevelId', sl.id,
                       'skillLevelName', sl.name
                   ) ORDER BY sp.name) AS sports
            FROM user_sports us
            JOIN sports sp ON sp.id = us.sport_id
            JOIN sif_skill_level sl ON sl.id = us.skill_level_id
            WHERE us.user_id = u.id
        ) s ON TRUE
        """;

    public List<UserResponse> search(Short cityId,
                                     Short sportId,
                                     Short skillLevelId,
                                     Short ageMin,
                                     Short ageMax) {
        StringBuilder sql = new StringBuilder(RESPONSE_SELECT).append(" WHERE 1=1");
        Map<String, Object> p = new HashMap<>();

        if (cityId != null) {
            sql.append(" AND u.city_id = :cityId");
            p.put("cityId", cityId);
        }

        if (sportId != null || skillLevelId != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM user_sports us2 WHERE us2.user_id = u.id");
            if (sportId != null) {
                sql.append(" AND us2.sport_id = :sportId");
                p.put("sportId", sportId);
            }
            if (skillLevelId != null) {
                sql.append(" AND us2.skill_level_id = :skillLevelId");
                p.put("skillLevelId", skillLevelId);
            }
            sql.append(")");
        }

        if (ageMin != null) {
            sql.append(" AND u.date_of_birth <= CURRENT_DATE - (:ageMin * INTERVAL '1 year')");
            p.put("ageMin", ageMin);
        }
        if (ageMax != null) {
            sql.append(" AND u.date_of_birth > CURRENT_DATE - ((:ageMax + 1) * INTERVAL '1 year')");
            p.put("ageMax", ageMax);
        }

        sql.append(" ORDER BY u.id");

        return jdbc.sql(sql.toString()).params(p).query(this::mapRow).list();
    }

    public UserResponse getById(Long id) {
        return jdbc.sql(RESPONSE_SELECT + " WHERE u.id = :id")
                .param("id", id)
                .query(this::mapRow)
                .optional()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Korisnik " + id + " ne postoji."));
    }

    public Optional<User> getUserById(Long id) {
        return jdbc.sql("""
            SELECT id as user_id, name, username, gender,
                   date_of_birth AS date_of_birth,
                   city_id
            FROM users WHERE id = :id
            """)
                .param("id", id)
                .query(User.class)
                .optional();
    }

    public void update(User u) {
        jdbc.sql("""
            UPDATE users
            SET username = :username,
                name = :name,
                gender = :gender,
                date_of_birth = :dateOfBirth,
                city_id = :cityId
            WHERE id = :userId
            """)
                .param("username", u.getUsername())
                .param("name", u.getName())
                .param("gender", u.getGender())
                .param("dateOfBirth", u.getDateOfBirth())
                .param("cityId", u.getCityId())
                .param("userId", u.getId())
                .update();

        if (u.getSports() != null) {
            jdbc.sql("DELETE FROM user_sports WHERE user_id = :id")
                    .param("id", u.getId())
                    .update();

            for (SportDto s : u.getSports()) {
                jdbc.sql("""
                    INSERT INTO user_sports (user_id, sport_id, skill_level_id)
                    VALUES (:userId, :sportId, :skillLevelId)
                    """)
                        .param("userId", u.getId())
                        .param("sportId", s.sportId())
                        .param("skillLevelId", s.skillLevelId())
                        .update();
            }
        }
    }
}
