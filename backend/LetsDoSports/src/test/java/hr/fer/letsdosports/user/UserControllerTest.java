package hr.fer.letsdosports.user;

import hr.fer.letsdosports.auth.CurrentUserArgumentResolver;
import hr.fer.letsdosports.auth.CurrentUserId;
import hr.fer.letsdosports.config.ApiPrefixConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiPrefixConfig.class)
class UserControllerTest {

    private static final long ME = 7L;
    private static final String BASE = "/api/v1/users";

    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;

    @MockitoBean UserService service;
    @MockitoBean CurrentUserArgumentResolver currentUserResolver;

    @BeforeEach
    void setUp() throws Exception {
        // Make the mocked resolver behave like the real one: handle @CurrentUserId, return user ME.
        when(currentUserResolver.supportsParameter(any()))
                .thenAnswer(inv -> ((org.springframework.core.MethodParameter) inv.getArgument(0))
                        .hasParameterAnnotation(CurrentUserId.class));
        when(currentUserResolver.resolveArgument(any(), any(), any(), any())).thenReturn(ME);
    }

    private UserResponse response(long id) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", "Ana Horvat");
        m.put("username", "ana");
        m.put("profilePhotoUrl", null);
        m.put("gender", "žensko");
        m.put("birthDate", "2002-03-14");
        m.put("age", 24);
        m.put("cityId", 1);
        m.put("appRoleId", "USER");
        m.put("sports", List.of(Map.of("sportId", 1, "skillLevelId", 2)));
        return json.convertValue(m, UserResponse.class);
    }

    private Map<String, Object> validCreateBody() {
        Map<String, Object> m = new HashMap<>();
        m.put("name", "Ana Horvat");
        m.put("username", "ana");
        m.put("gender", "žensko");
        m.put("birthDate", "2002-03-14");
        m.put("cityId", 1);
        m.put("sports", List.of(Map.of("sportId", 1, "skillLevelId", 2)));
        return m;
    }

    private String body(Object o) {
        return json.writeValueAsString(o);
    }

    // ------------------------------------------------------------- GET /users

    @Test
    void getAll_withoutFilters_passesNullsAndReturns200() throws Exception {
        when(service.search(null, null, null, null, null)).thenReturn(List.of(response(1), response(2)));

        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(service).search(null, null, null, null, null);
    }

    @Test
    void getAll_withAllFilters_passesThemToService() throws Exception {
        when(service.search((short) 1, (short) 2, (short) 1, (short) 18, (short) 30)).thenReturn(List.of());

        mvc.perform(get(BASE)
                        .param("cityId", "1")
                        .param("sportId", "2")
                        .param("skillLevelId", "1")
                        .param("ageMin", "18")
                        .param("ageMax", "30"))
                .andExpect(status().isOk());

        verify(service).search((short) 1, (short) 2, (short) 1, (short) 18, (short) 30);
    }

    @Test
    void getAll_emptyResult_returns200AndEmptyArray() throws Exception {
        when(service.search(any(), any(), any(), any(), any())).thenReturn(List.of());

        mvc.perform(get(BASE).param("cityId", "999"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getAll_skillLevelWithoutSport_returns400() throws Exception {
        // Spec: skillLevelId requires sportId -> 400. The service is expected to enforce it.
        when(service.search(null, null, (short) 1, null, null))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST));

        mvc.perform(get(BASE).param("skillLevelId", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAll_nonNumericFilter_returns400() throws Exception {
        mvc.perform(get(BASE).param("cityId", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    // ------------------------------------------------------- GET /users/{id}

    @Test
    void getOne_existing_returns200() throws Exception {
        when(service.getById(12L)).thenReturn(response(12));

        mvc.perform(get(BASE + "/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.username").value("ana"));
    }

    @Test
    void getOne_unknown_returns404() throws Exception {
        when(service.getById(999L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mvc.perform(get(BASE + "/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOne_nonNumericId_returns400() throws Exception {
        mvc.perform(get(BASE + "/abc"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------- GET /users/me

    @Test
    void getMe_returnsOwnProfileWithFlatModel() throws Exception {
        when(service.getMine(ME)).thenReturn(response(ME));

        mvc.perform(get(BASE + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ME))
                .andExpect(jsonPath("$.name").value("Ana Horvat"))
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.gender").value("žensko"))
                .andExpect(jsonPath("$.birthDate").value("2002-03-14"))
                .andExpect(jsonPath("$.cityId").value(1))
                .andExpect(jsonPath("$.appRoleId").value("USER"))
                .andExpect(jsonPath("$.sports[0].sportId").value(1))
                .andExpect(jsonPath("$.sports[0].skillLevelId").value(2));

        verify(service).getMine(ME);
        verify(service, never()).getById(any());   // "me" must not be treated as {id}
    }

    // -------------------------------------------------------- PATCH /users/me

    @Test
    void patchMe_partialUpdate_returns200WithUpdatedProfile() throws Exception {
        when(service.getById(ME)).thenReturn(response(ME));

        mvc.perform(patch(BASE + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Ana Novak"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ME));

        verify(service).update(eq(ME), any(UpdateUserRequest.class));
        verify(service).getById(ME);
    }

    @Test
    void patchMe_replacesSportsList() throws Exception {
        when(service.getById(ME)).thenReturn(response(ME));

        mvc.perform(patch(BASE + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("sports", List.of(Map.of("sportId", 3, "skillLevelId", 1))))))
                .andExpect(status().isOk());

        verify(service).update(eq(ME), any(UpdateUserRequest.class));
    }

    @Test
    void patchMe_emptySportsList_returns400() throws Exception {
        mvc.perform(patch(BASE + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("sports", List.of()))))
                .andExpect(status().isBadRequest());

        verify(service, never()).update(any(), any());
    }

    @Test
    void patchMe_nullForRequiredFields_returns400() throws Exception {
        for (String field : List.of("name", "username", "gender", "birthDate", "cityId", "sports")) {
            Map<String, Object> b = new HashMap<>();
            b.put(field, null);

            mvc.perform(patch(BASE + "/me").contentType(MediaType.APPLICATION_JSON).content(body(b)))
                    .andExpect(status().isBadRequest());
        }
        verify(service, never()).update(any(), any());
    }

    @Test
    void patchMe_blankGender_returns400() throws Exception {
        mvc.perform(patch(BASE + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("gender", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchMe_futureBirthDate_returns400() throws Exception {
        mvc.perform(patch(BASE + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("birthDate", LocalDate.now().plusDays(1).toString()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchMe_unknownOrForbiddenFields_returns400() throws Exception {
        for (String field : List.of("userId", "appRoleId", "profilePhotoUrl", "foo")) {
            mvc.perform(patch(BASE + "/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of(field, "x"))))
                    .andExpect(status().isBadRequest());
        }
        verify(service, never()).update(any(), any());
    }

    @Test
    void patchMe_duplicateUsername_returns409() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT))
                .when(service).update(eq(ME), any(UpdateUserRequest.class));

        mvc.perform(patch(BASE + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username", "taken"))))
                .andExpect(status().isConflict());
    }

    // -------------------------------------------------- PUT /users/me/photo


    @Test
    @Disabled("PUT /users/me/photo not implemented yet")
    void putPhoto_validFile_returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "me.png", "image/png", new byte[]{1, 2, 3});

        mvc.perform(multipart(BASE + "/me/photo").file(file).with(req -> {
                    req.setMethod("PUT");
                    return req;
                }))
                .andExpect(status().isOk());
    }
}