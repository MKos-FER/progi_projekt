package hr.fer.letsdosports.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Treba pokrenutu bazu (make db). Svaki test se vraća (rollback), baza ostaje nepromijenjena.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistrationIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;

    private static final String BODY = """
            {"name":"Test Korisnik","username":"test-registracija","gender":"Other",
             "birthDate":"2000-01-01","cityId":1,"sports":[{"sportId":1,"skillLevelId":2}]}""";

    private static RequestPostProcessor google(String subject) {
        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientId("x").redirectUri("http://localhost/cb")
                .authorizationUri("http://localhost/auth").tokenUri("http://localhost/token")
                .build();
        return oauth2Login().clientRegistration(google)
                .attributes(a -> { a.put("sub", subject); a.put("email", "test@example.com"); });
    }

    @Test
    void register_newGoogleAccount_createsUserSportsAndLink() throws Exception {
        mvc.perform(post("/api/v1/auth/register").with(google("new-sub")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        Long userId = jdbc.sql("SELECT user_id FROM oauth_accounts WHERE provider = 'google' AND provider_user_id = 'new-sub'")
                .query(Long.class).single();
        assertThat(jdbc.sql("SELECT username FROM users WHERE id = :id").param("id", userId)
                .query(String.class).single()).isEqualTo("test-registracija");
        assertThat(jdbc.sql("SELECT count(*) FROM user_sports WHERE user_id = :id").param("id", userId)
                .query(Integer.class).single()).isEqualTo(1);

        // Nakon registracije zaštićene rute prepoznaju korisnika
        mvc.perform(get("/api/v1/users/me/events").with(google("new-sub")))
                .andExpect(status().isOk());
    }

    @Test
    void protectedRoute_beforeRegistration_returns403() throws Exception {
        mvc.perform(get("/api/v1/users/me/events").with(google("unregistered-sub")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Registracija nije dovršena."));
    }

    @Test
    void register_withoutCsrf_returns403() throws Exception {
        mvc.perform(post("/api/v1/auth/register").with(google("new-sub"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_notLoggedIn_returns401() throws Exception {
        mvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_twice_returns409() throws Exception {
        mvc.perform(post("/api/v1/auth/register").with(google("twice-sub")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/auth/register").with(google("twice-sub")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("test-registracija", "drugi")))
                .andExpect(status().isConflict());
    }

    @Test
    void register_invalidData_returns400() throws Exception {
        mvc.perform(post("/api/v1/auth/register").with(google("bad-sub")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"gender\":\"x\",\"cityId\":9999,\"sports\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.gender").exists())
                .andExpect(jsonPath("$.errors.sports").exists());

        mvc.perform(post("/api/v1/auth/register").with(google("bad-sub")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("\"cityId\":1", "\"cityId\":9999")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cityId").value("Grad ne postoji."));
    }
}
