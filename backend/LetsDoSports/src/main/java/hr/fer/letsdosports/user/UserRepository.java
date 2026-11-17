package hr.fer.letsdosports.user;

import hr.fer.letsdosports.sport.Sport;
import hr.fer.letsdosports.sport.SportDto;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.*;

@Repository
public class UserRepository {
    private final JdbcClient jdbc;
    public UserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static final String RESPONSE_SELECT = """
        SELECT u.id,
               u.name,
               u.username,
               u.profile_photo_url,
               u.gender,
               u.date_of_birth AS birth_date,
               EXTRACT(YEAR FROM age(u.date_of_birth))::int AS age,
               c.name AS city_name,
               u.app_role_id,
               COALESCE(s.sports, '[]'::json) AS sports,
               u.created_at
        FROM users u
        LEFT JOIN sif_city c ON c.id = u.city_id
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

        return jdbc.sql(sql.toString()).params(p).query(UserResponse.class).list();
    }

    public UserResponse getById(Long id){
        StringBuilder sql = new StringBuilder(RESPONSE_SELECT).append(" WHERE u.id=:id");
        Map<String, Object> p = new HashMap<>();
        p.put("id", id);
        return jdbc.sql(sql.toString()).params(p).query(UserResponse.class).single();
    }

    public Optional<User> getUserById(Long id){
        return jdbc.sql("""
        SELECT * FROM users WHERE id = :id
""" ).param("id", id).query(User.class).optional();
    }

    public Optional<User> update(User u){
        User u2 = jdbc.sql("""
        UPDATE users SET username = :username, name = :name, gender = :gender, date_of_birth = :birthDate, city_id = cityId WHERE id = :id RETURNING *
""").paramSource(u).query(User.class).single();
        for (SportDto s : u.getSports()){
            jdbc.sql("""
            UPDATE user_sports SET sport_id = :sportId, skill_level_id = :skillLevelId where id = :id
        """);
        }
        return getUserById(u.getId());
    }
}
