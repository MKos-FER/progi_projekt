package hr.fer.letsdosports.user;

import hr.fer.letsdosports.user.application.UserSearchRepository;
import hr.fer.letsdosports.user.domain.UserSummary;
import hr.fer.letsdosports.user.domain.UserSearchFilter;
import hr.fer.letsdosports.security.ApplicationUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "LDS_INTEGRATION_TESTS", matches = "true")
class JdbcUserSearchRepositoryIT {
    @Autowired
    private UserSearchRepository repository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private MockMvc mvc;

    private int city;
    private int otherCity;
    private int football;
    private int basketball;
    private long ana;
    private long ivan;
    private long petra;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);

    @BeforeEach
    void fixtures() {
        city = jdbc.queryForObject("INSERT INTO sif_city (name) VALUES ('F005 test city') RETURNING id", Integer.class);
        otherCity = jdbc.queryForObject("INSERT INTO sif_city (name) VALUES ('F005 other city') RETURNING id", Integer.class);
        football = jdbc.queryForObject("INSERT INTO sports (name) VALUES ('F005 football') RETURNING id", Integer.class);
        basketball = jdbc.queryForObject("INSERT INTO sports (name) VALUES ('F005 basketball') RETURNING id", Integer.class);
        ana = user("f005-ana", "2000-11-12", city);
        ivan = user("f005-ivan", "2000-11-13", city);
        petra = user("f005-petra", "2000-11-11", otherCity);
        sport(ana, football, 2);
        sport(ana, basketball, 1);
        sport(ivan, football, 1);
        sport(ivan, basketball, 2);
        sport(petra, football, 2);
    }

    @Test
    void httpReadsDatabaseThroughAuthenticatedSession() throws Exception {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new ApplicationUserPrincipal(ana), null, List.of()));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        mvc.perform(get("/api/v1/users").session(session)
                        .param("sportId", Integer.toString(football)).param("cityId", Integer.toString(city))
                        .param("skillLevelId", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ana)).andExpect(jsonPath("$[0].sports.length()").value(2))
                .andExpect(jsonPath("$[0].birthDate").doesNotExist())
                .andExpect(jsonPath("$[0].gender").doesNotExist());
        mvc.perform(get("/api/v1/users/{id}", ana).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("f005-ana"));
    }

    @Test
    void combinesCriteriaForSameSportAndReturnsAllSports() {
        jdbc.update("UPDATE users SET profile_photo_url = ? WHERE id = ?", "https://example.com/ana.jpg", ana);
        var users = repository.search(new UserSearchFilter(football, city, 26, 26, 2), TODAY);
        assertThat(users).extracting(UserSummary::id).containsExactly(ana);
        var user = users.getFirst();
        assertThat(user.sports()).extracting(UserSummary.Sport::sportId).containsExactly(football, basketball);
        assertThat(user.sports()).extracting(UserSummary.Sport::skillLevelId).containsExactly(2, 1);
        assertThat(user.city()).isEqualTo(new UserSummary.City(city, "F005 test city"));
        assertThat(user.profilePhotoUrl()).isEqualTo("https://example.com/ana.jpg");
        assertThat(repository.findById(ana, TODAY)).contains(user);
    }

    @Test
    void appliesEachOptionalFilterAndStableOrderingWithoutDuplicates() {
        assertThat(repository.search(new UserSearchFilter(football, null, null, null, null), TODAY))
                .extracting(UserSummary::id).containsExactly(ana, ivan, petra);
        assertThat(repository.search(new UserSearchFilter(null, city, null, null, null), TODAY))
                .extracting(UserSummary::id).containsExactly(ana, ivan);
        assertThat(repository.search(new UserSearchFilter(football, null, null, null, 2), TODAY))
                .extracting(UserSummary::id).containsExactly(ana, petra);
        assertThat(repository.search(new UserSearchFilter(football, null, 26, null, null), TODAY))
                .extracting(UserSummary::id).containsExactly(ana, petra);
        assertThat(repository.search(new UserSearchFilter(football, null, null, 25, null), TODAY))
                .extracting(UserSummary::id).containsExactly(ivan);
        var all = repository.search(new UserSearchFilter(null, null, null, null, null), TODAY);
        assertThat(all).extracting(UserSummary::id).isSorted().doesNotHaveDuplicates().contains(ana, ivan, petra);
    }

    @Test
    void returnsEmptyForUnknownReferencesAndMissingUser() {
        assertThat(repository.search(new UserSearchFilter(32767, null, null, null, null), TODAY)).isEmpty();
        assertThat(repository.search(new UserSearchFilter(null, 32767, null, null, null), TODAY)).isEmpty();
        assertThat(repository.search(new UserSearchFilter(football, null, null, null, 32767), TODAY)).isEmpty();
        assertThat(repository.findById(Long.MAX_VALUE, TODAY)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"2026-11-11,25", "2026-11-12,26", "2026-11-13,26",
            "2025-02-28,24", "2025-03-01,25", "2024-02-28,23", "2024-02-29,24", "2024-03-01,24"})
    void ageAndInclusiveFiltersUseSameReferenceDate(LocalDate today, int age) {
        boolean leapBirthday = today.getMonthValue() < 4;
        jdbc.update("UPDATE users SET date_of_birth = ? WHERE id = ?",
                Date.valueOf(leapBirthday ? LocalDate.of(2000, 2, 29) : LocalDate.of(2000, 11, 12)), ana);
        assertThat(repository.findById(ana, today).orElseThrow().age()).isEqualTo(age);
        assertThat(repository.search(new UserSearchFilter(football, city, age, age, 2), today))
                .extracting(UserSummary::id).containsExactly(ana);
        assertThat(repository.search(new UserSearchFilter(football, city, age + 1, null, 2), today)).isEmpty();
        assertThat(repository.search(new UserSearchFilter(football, city, null, age - 1, 2), today)).isEmpty();
    }

    private long user(String username, String birthDate, int cityId) {
        return jdbc.queryForObject("""
                INSERT INTO users (username, name, gender, date_of_birth, city_id)
                VALUES (?, 'F005 user', 'test', ?, ?) RETURNING id
                """, Long.class, username, Date.valueOf(birthDate), cityId);
    }

    private void sport(long userId, int sportId, int skillLevel) {
        jdbc.update("INSERT INTO user_sports (user_id, sport_id, skill_level_id) VALUES (?, ?, ?)",
                userId, sportId, skillLevel);
    }
}
