package hr.fer.letsdosports.user;

import hr.fer.letsdosports.api.ApiExceptionHandler;
import hr.fer.letsdosports.user.application.DefaultUserSearchService;
import hr.fer.letsdosports.user.application.UserSearchRepository;
import hr.fer.letsdosports.user.controller.UserSearchController;
import hr.fer.letsdosports.user.domain.UserSummary;
import hr.fer.letsdosports.user.domain.UserSearchFilter;
import hr.fer.letsdosports.security.ApiSecurityConfiguration;
import hr.fer.letsdosports.security.ApplicationUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserSearchController.class, properties = "lds.cors.allowed-origins=http://localhost:5173")
@Import({ApiSecurityConfiguration.class, ApiExceptionHandler.class, DefaultUserSearchService.class})
class UserSearchControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private UserSearchRepository repository;

    private final UserSummary user = new UserSummary(12, "ana", "Ana", null, 24,
            new UserSummary.City(1, "Zagreb"), List.of(new UserSummary.Sport(1, "Nogomet", 2, "Average")));

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/users", "/api/v1/users/12"})
    void requiresLocalUserSession(String url) throws Exception {
        mvc.perform(get(url)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get(url).session(session("12"))).andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }

    @Test
    void listAndDetailExposeOnlyContractFields() throws Exception {
        when(repository.search(any(), any())).thenReturn(List.of(user));
        when(repository.findById(eq(12L), any())).thenReturn(Optional.of(user));
        String json = """
                {"id":12,"username":"ana","name":"Ana","profilePhotoUrl":null,"age":24,
                 "city":{"id":1,"name":"Zagreb"},
                 "sports":[{"sportId":1,"sportName":"Nogomet","skillLevelId":2,"skillLevelName":"Average"}]}
                """;
        mvc.perform(get("/api/v1/users").session(localSession()))
                .andExpect(status().isOk()).andExpect(content().json("[" + json + "]", org.springframework.test.json.JsonCompareMode.STRICT));
        mvc.perform(get("/api/v1/users/12").session(localSession()))
                .andExpect(status().isOk()).andExpect(content().json(json, org.springframework.test.json.JsonCompareMode.STRICT));
    }

    @Test
    void combinesFiltersAndReturnsEmptyArray() throws Exception {
        var filter = new UserSearchFilter(1, 2, 18, 30, 3);
        when(repository.search(eq(filter), any())).thenReturn(List.of());
        mvc.perform(get("/api/v1/users?sportId=1&cityId=2&ageMin=18&ageMax=30&skillLevelId=3").session(localSession()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(repository).search(eq(filter), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"?skillLevelId=1", "?cityId=0", "?sportId=32768", "?skillLevelId=-1&sportId=1",
            "?ageMin=-1", "?ageMax=-1", "?ageMin=30&ageMax=18", "?ageMin=2.5", "?cityId=abc",
            "?ageMax=2147483648", "/0", "/-1", "/9223372036854775808"})
    void rejectsInvalidInput(String suffix) throws Exception {
        mvc.perform(get("/api/v1/users" + suffix).session(localSession()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(repository);
    }

    @Test
    void returns404ForMissingUser() throws Exception {
        when(repository.findById(eq(99L), any())).thenReturn(Optional.empty());
        mvc.perform(get("/api/v1/users/99").session(localSession()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void preservesCsrfAndDeniesOtherRoutes() throws Exception {
        mvc.perform(post("/api/v1/users").session(localSession()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mvc.perform(get("/other").session(localSession())).andExpect(status().isForbidden());
    }

    @Test
    void allowsOnlyConfiguredCorsOriginsWithCredentials() throws Exception {
        when(repository.search(any(), any())).thenReturn(List.of());
        mvc.perform(get("/api/v1/users").session(localSession()).header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(get("/api/v1/users").session(localSession()).header("Origin", "https://untrusted.example"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PATCH"})
    void allowsWritePreflightOnlyFromConfiguredOrigins(String method) throws Exception {
        mvc.perform(options("/api/v1/users").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", method)
                        .header("Access-Control-Request-Headers", "Content-Type,X-CSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString(method)));
        mvc.perform(options("/api/v1/users").header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", method))
                .andExpect(status().isForbidden());
        mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), "/api/v1/users")
                        .session(localSession()).header("Origin", "http://localhost:5173"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verifyNoInteractions(repository);
    }

    private static MockHttpSession localSession() {
        return session(new ApplicationUserPrincipal(12));
    }

    private static MockHttpSession session(Object principal) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
