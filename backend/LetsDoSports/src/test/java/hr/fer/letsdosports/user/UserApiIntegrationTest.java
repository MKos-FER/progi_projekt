package hr.fer.letsdosports.user;

import hr.fer.letsdosports.auth.CurrentUserArgumentResolver;
import hr.fer.letsdosports.auth.CurrentUserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class UserApiIntegrationTest {

    private static final String BASE = "/api/v1/users";
    private static final String APP_ROLE = "USER";

    @Autowired
    DataSource dataSource;

    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired JdbcClient jdbc;

    @MockitoBean CurrentUserArgumentResolver currentUserResolver;

    private long currentUserId;

    private short city1, city2;
    private short sport1, sport2, sport3;
    private short level1, level2;
    private long ana, ivan;

    private void assertLocalDatabase() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();

            assertThat(url)
                    .as("Tests must only run against the local development database")
                    .startsWith("jdbc:postgresql://localhost:5432/lets_do_sports");

            assertThat(connection.getCatalog())
                    .isEqualTo("lets_do_sports");
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        assertLocalDatabase();
        when(currentUserResolver.supportsParameter(any()))
                .thenAnswer(inv -> ((MethodParameter) inv.getArgument(0))
                        .hasParameterAnnotation(CurrentUserId.class));
        when(currentUserResolver.resolveArgument(any(), any(), any(), any()))
                .thenAnswer(inv -> currentUserId);

        List<Short> cities = lookupIds("sif_city", 2);
        List<Short> sports = lookupIds("sports", 3);
        List<Short> levels = lookupIds("sif_skill_level", 2);
        city1 = cities.get(0);  city2 = cities.get(1);
        sport1 = sports.get(0); sport2 = sports.get(1); sport3 = sports.get(2);
        level1 = levels.get(0); level2 = levels.get(1);


        ana = insertUser("Ana Horvat", "ana_it", LocalDate.of(2000, 1, 1), city1);
        addSport(ana, sport1, level1);
        addSport(ana, sport2, level2);


        ivan = insertUser("Ivan Kovač", "ivan_it", LocalDate.of(1990, 5, 5), city2);
        addSport(ivan, sport1, level2);

        currentUserId = ana;
    }

    // ================================================================ GET /users

    @Test
    void search_noFilters_returnsAllUsersWithTheirSports() throws Exception {
        List<UserResponse> result = search(Map.of());

        assertThat(result).extracting(UserResponse::id).contains(ana, ivan);
        UserResponse a = byId(result, ana);
        assertThat(a.username()).isEqualTo("ana_it");
        assertThat(a.sports()).hasSize(2);
        assertThat(a.sports()).allSatisfy(s -> {
            assertThat(s.sportName()).isNotBlank();
            assertThat(s.skillLevelName()).isNotBlank();
        });
    }

    @Test
    void search_byCity_returnsOnlyUsersFromThatCity() throws Exception {
        List<UserResponse> result = search(Map.of("cityId", city1));

        assertThat(result).extracting(UserResponse::id).contains(ana).doesNotContain(ivan);
        assertThat(result).allSatisfy(u -> assertThat(u.cityId()).isEqualTo(city1));
    }

    @Test
    void search_byUnknownCity_returnsEmptyArray() throws Exception {
        mvc.perform(get(BASE).param("cityId", "32767"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void search_bySport_returnsEveryoneWhoPlaysIt() throws Exception {
        assertThat(search(Map.of("sportId", sport1))).extracting(UserResponse::id).contains(ana, ivan);
        assertThat(search(Map.of("sportId", sport2))).extracting(UserResponse::id).contains(ana).doesNotContain(ivan);
    }

    @Test
    void search_bySportAndSkill_matchesOnTheSameSportRow() throws Exception {

        assertThat(search(Map.of("sportId", sport1, "skillLevelId", level2)))
                .extracting(UserResponse::id).contains(ivan).doesNotContain(ana);

        assertThat(search(Map.of("sportId", sport2, "skillLevelId", level1)))
                .extracting(UserResponse::id).doesNotContain(ana, ivan);
    }

    @Test
    void search_skillLevelWithoutSport_returns400() throws Exception {
        mvc.perform(get(BASE).param("skillLevelId", String.valueOf(level1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void search_nonNumericFilter_returns400() throws Exception {
        mvc.perform(get(BASE).param("cityId", "abc")).andExpect(status().isBadRequest());
        mvc.perform(get(BASE).param("ageMin", "x")).andExpect(status().isBadRequest());
    }

    @Test
    void search_ageBoundaries_areInclusiveAndExact() throws Exception {
        LocalDate today = LocalDate.now();
        long turned18Today = insertUser("Born18", "born18_it", today.minusYears(18), city1);
        long turns18Tomorrow = insertUser("Almost18", "almost18_it", today.minusYears(18).plusDays(1), city1);


        assertThat(search(Map.of("ageMin", 18))).extracting(UserResponse::id)
                .contains(turned18Today).doesNotContain(turns18Tomorrow);
        assertThat(search(Map.of("ageMax", 18))).extracting(UserResponse::id)
                .contains(turned18Today, turns18Tomorrow);
        assertThat(search(Map.of("ageMin", 18, "ageMax", 18))).extracting(UserResponse::id)
                .contains(turned18Today).doesNotContain(turns18Tomorrow, ana, ivan);
        assertThat(search(Map.of("ageMax", 17))).extracting(UserResponse::id)
                .contains(turns18Tomorrow).doesNotContain(turned18Today);


        assertThat(byId(search(Map.of("ageMin", 18, "ageMax", 18)), turned18Today).age()).isEqualTo(18);
    }

    @Test
    void search_ageRangeNarrowsResults() throws Exception {
        int anaAge = byId(search(Map.of()), ana).age();
        assertThat(search(Map.of("ageMin", anaAge, "ageMax", anaAge)))
                .extracting(UserResponse::id).contains(ana).doesNotContain(ivan);
    }

    @Test
    void search_absurdAge_returns400() throws Exception {
        mvc.perform(get(BASE).param("ageMin", "32767")).andExpect(status().isBadRequest());
        mvc.perform(get(BASE).param("ageMax", "32767")).andExpect(status().isBadRequest());
    }

    @Test
    void search_negativeAge_returns400() throws Exception {
        mvc.perform(get(BASE).param("ageMin", "-5")).andExpect(status().isBadRequest());
    }

    @Test
    void search_ageMinGreaterThanAgeMax_returns400() throws Exception {
        mvc.perform(get(BASE).param("ageMin", "40").param("ageMax", "20"))
                .andExpect(status().isBadRequest());
    }

    // ============================================================ GET /users/{id}

    @Test
    void getOne_existing_returnsFullProfile() throws Exception {
        mvc.perform(get(BASE + "/" + ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) ana))
                .andExpect(jsonPath("$.name").value("Ana Horvat"))
                .andExpect(jsonPath("$.username").value("ana_it"))
                .andExpect(jsonPath("$.gender").value("Female"))
                .andExpect(jsonPath("$.birthDate").value("2000-01-01"))
                .andExpect(jsonPath("$.cityId").value((int) city1))
                .andExpect(jsonPath("$.age").isNumber())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.sports.length()").value(2))
                .andExpect(jsonPath("$.sports[?(@.sportId==" + sport1 + ")].skillLevelId").value((int) level1))
                .andExpect(jsonPath("$.sports[?(@.sportId==" + sport2 + ")].skillLevelId").value((int) level2));
    }

    @Test
    void getOne_userWithoutSports_returnsEmptySportsArray() throws Exception {
        long lonely = insertUser("No Sports", "nosports_it", LocalDate.of(1995, 1, 1), city1);

        mvc.perform(get(BASE + "/" + lonely))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sports").isArray())
                .andExpect(jsonPath("$.sports.length()").value(0));
    }

    @Test
    void getOne_unknown_returns404WithMessage() throws Exception {
        mvc.perform(get(BASE + "/999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Korisnik 999999999 ne postoji."));
    }

    @Test
    void getOne_nonNumericId_returns400() throws Exception {
        mvc.perform(get(BASE + "/abc")).andExpect(status().isBadRequest());
    }

    // ============================================================== GET /users/me

    @Test
    void getMe_returnsCurrentUser_notTreatedAsId() throws Exception {
        currentUserId = ivan;

        mvc.perform(get(BASE + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) ivan))
                .andExpect(jsonPath("$.username").value("ivan_it"));
    }

    @Test
    void getMe_whenUserRowIsGone_returns404() throws Exception {
        currentUserId = 999_999_999L;

        mvc.perform(get(BASE + "/me")).andExpect(status().isNotFound());
    }

    // ============================================================ PATCH /users/me

    @Test
    void patch_partialUpdate_changesOnlyGivenFieldsAndPersists() throws Exception {
        patchMe(Map.of("name", "Ana Novak"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) ana))
                .andExpect(jsonPath("$.name").value("Ana Novak"))
                .andExpect(jsonPath("$.username").value("ana_it"))
                .andExpect(jsonPath("$.birthDate").value("2000-01-01"))
                .andExpect(jsonPath("$.cityId").value((int) city1))
                .andExpect(jsonPath("$.sports.length()").value(2));

        assertThat(dbString("name", ana)).isEqualTo("Ana Novak");
        assertThat(dbSportIds(ana)).containsExactlyInAnyOrder(sport1, sport2);
    }

    @Test
    void patch_allFields_updatesEverything() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Ana Nova");
        body.put("username", "ana_new_it");
        body.put("gender", "Other");
        body.put("birthDate", "1999-12-31");
        body.put("cityId", city2);
        body.put("sports", List.of(Map.of("sportId", sport3, "skillLevelId", level2)));

        patchMe(body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Nova"))
                .andExpect(jsonPath("$.username").value("ana_new_it"))
                .andExpect(jsonPath("$.gender").value("Other"))
                .andExpect(jsonPath("$.birthDate").value("1999-12-31"))
                .andExpect(jsonPath("$.cityId").value((int) city2))
                .andExpect(jsonPath("$.sports.length()").value(1))
                .andExpect(jsonPath("$.sports[0].sportId").value((int) sport3))
                .andExpect(jsonPath("$.sports[0].skillLevelId").value((int) level2));
    }

    @Test
    void patch_sports_replacesWholeListInDb() throws Exception {
        patchMe(Map.of("sports", List.of(Map.of("sportId", sport3, "skillLevelId", level1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sports.length()").value(1));

        assertThat(dbSportIds(ana)).containsExactly(sport3);
        assertThat(dbSportIds(ivan)).containsExactly(sport1);
    }

    @Test
    void patch_clientSuppliedSportNamesAreIgnored() throws Exception {
        patchMe(Map.of("sports", List.of(Map.of(
                "sportId", sport3, "skillLevelId", level1,
                "sportName", "HACKED", "skillLevelName", "HACKED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sports[0].sportName").value(org.hamcrest.Matchers.not("HACKED")))
                .andExpect(jsonPath("$.sports[0].skillLevelName").value(org.hamcrest.Matchers.not("HACKED")));
    }

    @Test
    void patch_emptyBody_isNoOp() throws Exception {
        patchMe(Map.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Horvat"));

        assertThat(dbSportIds(ana)).containsExactlyInAnyOrder(sport1, sport2);
    }

    @Test
    void patch_ownCurrentUsername_isNotAConflict() throws Exception {
        patchMe(Map.of("username", "ana_it")).andExpect(status().isOk());
    }

    @Test
    void patch_usernameTakenByAnotherUser_returns409AndChangesNothing() throws Exception {
        patchMe(Map.of("name", "Should Not Persist", "username", "ivan_it"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username zauzet"));

        assertThat(dbString("name", ana)).isEqualTo("Ana Horvat");
        assertThat(dbString("username", ana)).isEqualTo("ana_it");
    }

    @Test
    void patch_unknownCity_returns400WithFieldError() throws Exception {
        patchMe(Map.of("cityId", 32767))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cityId").value("Grad ne postoji"));
    }

    @Test
    void patch_unknownSportAndSkillLevel_return400WithFieldErrors() throws Exception {
        patchMe(Map.of("sports", List.of(Map.of("sportId", 32767, "skillLevelId", 32766))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['sportId:32767']").exists())
                .andExpect(jsonPath("$.errors['skillLevelId:32766']").exists());
    }

    @Test
    void patch_duplicateSportInList_returns400() throws Exception {
        patchMe(Map.of("sports", List.of(
                Map.of("sportId", sport1, "skillLevelId", level1),
                Map.of("sportId", sport1, "skillLevelId", level2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['sportId:" + sport1 + "']").exists());

        assertThat(dbSportIds(ana)).containsExactlyInAnyOrder(sport1, sport2);
    }

    @Test
    void patch_validNameButInvalidSport_isAtomic() throws Exception {
        patchMe(Map.of("name", "Half Applied",
                "sports", List.of(Map.of("sportId", 32767, "skillLevelId", level1))))
                .andExpect(status().isBadRequest());

        assertThat(dbString("name", ana)).isEqualTo("Ana Horvat");
        assertThat(dbSportIds(ana)).containsExactlyInAnyOrder(sport1, sport2);
    }

    @Test
    void patch_emptySportsList_returns400() throws Exception {
        patchMe(Map.of("sports", List.of())).andExpect(status().isBadRequest());
        assertThat(dbSportIds(ana)).hasSize(2);
    }

    @Test
    void patch_sportEntryMissingIds_returns400WithNestedFieldPath() throws Exception {
        patchMe(Map.of("sports", List.of(Map.of("sportId", sport1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['sports[0].skillLevelId']").exists());
    }

    @Test
    void patch_explicitNullOnAnyField_returns400() throws Exception {
        for (String field : List.of("name", "username", "gender", "birthDate", "cityId", "sports")) {
            Map<String, Object> body = new HashMap<>();
            body.put(field, null);
            patchMe(body).andExpect(status().isBadRequest());
        }
        assertThat(dbString("name", ana)).isEqualTo("Ana Horvat");
    }

    @Test
    void patch_invalidGender_returns400() throws Exception {
        patchMe(Map.of("gender", "")).andExpect(status().isBadRequest());
        patchMe(Map.of("gender", "x")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.gender").exists());
    }

    @Test
    void patch_futureBirthDate_returns400() throws Exception {
        patchMe(Map.of("birthDate", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.birthDate").exists());
    }

    @Test
    void patch_malformedJsonOrWrongTypes_return400() throws Exception {
        mvc.perform(patch(BASE + "/me").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.body").exists());

        patchMe(Map.of("cityId", "abc")).andExpect(status().isBadRequest());
        patchMe(Map.of("cityId", 99999)).andExpect(status().isBadRequest());
        patchMe(Map.of("cityId", 0)).andExpect(status().isBadRequest());
        patchMe(Map.of("birthDate", "14.03.2002")).andExpect(status().isBadRequest());
    }

    @Test
    void patch_wrongContentType_returns415() throws Exception {
        mvc.perform(patch(BASE + "/me").contentType(MediaType.TEXT_PLAIN).content("name=x"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void patch_unknownOrForbiddenFields_return400() throws Exception {
        for (String field : List.of("id", "userId", "appRoleId", "profilePhotoUrl", "foo")) {
            patchMe(Map.of(field, "x")).andExpect(status().isBadRequest());
        }
        assertThat(dbString("app_role_id::text", ana)).isEqualTo(APP_ROLE);
    }

    @Test
    void patch_whitespaceOnlyNameOrUsername_returns400() throws Exception {
        patchMe(Map.of("name", "   ")).andExpect(status().isBadRequest());
        patchMe(Map.of("username", "   ")).andExpect(status().isBadRequest());
        patchMe(Map.of("name", "")).andExpect(status().isBadRequest());
    }

    @Test
    void patch_onlyAffectsCurrentUser() throws Exception {
        currentUserId = ivan;

        patchMe(Map.of("name", "Ivan Changed")).andExpect(status().isOk());

        assertThat(dbString("name", ivan)).isEqualTo("Ivan Changed");
        assertThat(dbString("name", ana)).isEqualTo("Ana Horvat");
    }

    @Test
    void patch_whenUserRowIsGone_returns404() throws Exception {
        currentUserId = 999_999_999L;

        patchMe(Map.of("name", "Ghost")).andExpect(status().isNotFound());
    }

    // ===================================================================== helpers

    private ResultActions patchMe(Object body) throws Exception {
        return mvc.perform(patch(BASE + "/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private List<UserResponse> search(Map<String, Object> params) throws Exception {
        var req = get(BASE);
        params.forEach((k, v) -> req.param(k, String.valueOf(v)));
        String body = mvc.perform(req).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return List.of(json.readValue(body, UserResponse[].class));
    }

    private static UserResponse byId(List<UserResponse> users, long id) {
        return users.stream().filter(u -> u.id() == id).findFirst()
                .orElseThrow(() -> new AssertionError("user " + id + " not in result"));
    }

    private List<Short> lookupIds(String table, int min) {
        List<Short> ids = jdbc.sql("SELECT id FROM " + table + " ORDER BY id LIMIT " + min)
                .query(Short.class).list();
        assertThat(ids).as("test needs at least %d rows in %s (seed them in a migration)", min, table)
                .hasSize(min);
        return ids;
    }


    private long insertUser(String name, String username, LocalDate birthDate, short cityId) {
        return jdbc.sql("""
                INSERT INTO users (name, username, gender, date_of_birth, city_id, app_role_id)
                VALUES (:name, :username, 'Female', :dob, :cityId, :role)
                RETURNING id
                """)
                .param("name", name)
                .param("username", username)
                .param("dob", birthDate)
                .param("cityId", cityId)
                .param("role", APP_ROLE)
                .query(Long.class).single();
    }

    private void addSport(long userId, short sportId, short skillLevelId) {
        jdbc.sql("INSERT INTO user_sports (user_id, sport_id, skill_level_id) VALUES (:u, :s, :l)")
                .param("u", userId).param("s", sportId).param("l", skillLevelId).update();
    }

    private String dbString(String column, long userId) {
        return jdbc.sql("SELECT " + column + " FROM users WHERE id = :id")
                .param("id", userId).query(String.class).single();
    }

    private List<Short> dbSportIds(long userId) {
        return jdbc.sql("SELECT sport_id FROM user_sports WHERE user_id = :id")
                .param("id", userId).query(Short.class).list();
    }
}