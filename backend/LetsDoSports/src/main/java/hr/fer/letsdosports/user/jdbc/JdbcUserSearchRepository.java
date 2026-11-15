package hr.fer.letsdosports.user.jdbc;

import hr.fer.letsdosports.user.application.UserSearchRepository;
import hr.fer.letsdosports.user.domain.UserSummary;
import hr.fer.letsdosports.user.domain.UserSearchFilter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcUserSearchRepository implements UserSearchRepository {
    private static final String SELECT = """
            SELECT u.id, u.username, u.name, u.profile_photo_url,
                   EXTRACT(YEAR FROM age(CAST(:today AS date), u.date_of_birth))::integer AS age,
                   c.id AS city_id, c.name AS city_name,
                   s.id AS sport_id, s.name AS sport_name,
                   l.id AS skill_level_id, l.name AS skill_level_name
            FROM users u
            JOIN sif_city c ON c.id = u.city_id
            LEFT JOIN user_sports us ON us.user_id = u.id
            LEFT JOIN sports s ON s.id = us.sport_id
            LEFT JOIN sif_skill_level l ON l.id = us.skill_level_id
            WHERE TRUE
            """;
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcUserSearchRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<UserSummary> search(UserSearchFilter filter, LocalDate today) {
        var sql = new StringBuilder(SELECT);
        var parameters = new MapSqlParameterSource("today", Date.valueOf(today));
        if (filter.cityId() != null) {
            sql.append(" AND u.city_id = :cityId");
            parameters.addValue("cityId", filter.cityId());
        }
        if (filter.sportId() != null) {
            sql.append(
                    " AND EXISTS (SELECT 1 FROM user_sports matched WHERE matched.user_id = u.id AND matched.sport_id = :sportId");
            parameters.addValue("sportId", filter.sportId());
            if (filter.skillLevelId() != null) {
                sql.append(" AND matched.skill_level_id = :skillLevelId");
                parameters.addValue("skillLevelId", filter.skillLevelId());
            }
            sql.append(")");
        }
        if (filter.ageMin() != null) {
            sql.append(
                    " AND EXTRACT(YEAR FROM age(CAST(:today AS date), u.date_of_birth)) >= :ageMin");
            parameters.addValue("ageMin", filter.ageMin());
        }
        if (filter.ageMax() != null) {
            sql.append(
                    " AND EXTRACT(YEAR FROM age(CAST(:today AS date), u.date_of_birth)) <= :ageMax");
            parameters.addValue("ageMax", filter.ageMax());
        }
        sql.append(" ORDER BY u.id, s.id");
        return jdbc.query(sql.toString(), parameters, JdbcUserSearchRepository::readUsers);
    }

    @Override
    public Optional<UserSummary> findById(long id, LocalDate today) {
        var parameters = new MapSqlParameterSource("today", Date.valueOf(today)).addValue("id", id);
        var users = jdbc.query(SELECT + " AND u.id = :id ORDER BY s.id", parameters,
                JdbcUserSearchRepository::readUsers);
        return users.stream().findFirst();
    }

    private static List<UserSummary> readUsers(ResultSet rows) throws SQLException {
        var users = new ArrayList<UserSummary>();
        var sports = new ArrayList<UserSummary.Sport>();
        UserSummary current = null;
        while (rows.next()) {
            long id = rows.getLong("id");
            if (current == null || current.id() != id) {
                if (current != null) {
                    users.add(withSports(current, sports));
                }
                sports.clear();
                current = new UserSummary(id, rows.getString("username"), rows.getString("name"),
                        rows.getString("profile_photo_url"), rows.getInt("age"),
                        new UserSummary.City(rows.getInt("city_id"), rows.getString("city_name")),
                        List.of());
            }
            if (rows.getObject("sport_id") != null) {
                sports.add(
                        new UserSummary.Sport(rows.getInt("sport_id"), rows.getString("sport_name"),
                                rows.getInt("skill_level_id"), rows.getString("skill_level_name")));
            }
        }
        if (current != null) {
            users.add(withSports(current, sports));
        }
        return List.copyOf(users);
    }

    private static UserSummary withSports(UserSummary user, List<UserSummary.Sport> sports) {
        return new UserSummary(user.id(), user.username(), user.name(), user.profilePhotoUrl(),
                user.age(), user.city(), sports);
    }
}
