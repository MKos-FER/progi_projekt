package hr.fer.letsdosports.event;

import hr.fer.letsdosports.auth.CurrentUserArgumentResolver;
import hr.fer.letsdosports.auth.CurrentUserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({EventController.class, MyEventsController.class})
@AutoConfigureMockMvc(addFilters = false)
class EventControllerTest {

    private static final long USER_ID = 1L;

    @Autowired MockMvc mvc;

    @MockitoBean EventService service;
    @MockitoBean CurrentUserArgumentResolver currentUser;

    @BeforeEach
    void stubResolver() throws Exception {
        when(currentUser.supportsParameter(any())).thenAnswer(inv ->
                ((MethodParameter) inv.getArgument(0)).hasParameterAnnotation(CurrentUserId.class));
        when(currentUser.resolveArgument(any(), any(), any(), any())).thenReturn(USER_ID);
    }

    private static EventResponse sample(long id, String name) {
        return new EventResponse(id, name, "opis", (short) 1, 10L,
                OffsetDateTime.parse("2099-01-01T10:00:00Z"), 60, 10,
                (short) 18, null, null, OffsetDateTime.parse("2026-01-01T10:00:00Z"),
                "open", USER_ID, "ana", 1L);
    }

    private static final String VALID_CREATE_BODY = """
            {"name":"Petak nogomet","sportId":1,"venueId":10,
             "scheduledAt":"2099-01-01T10:00:00Z","maxParticipants":10,"minAge":18}""";

    // ---------- GET /events ----------

    @Test
    void getAll_returnsEvents() throws Exception {
        when(service.search(any(), any(), any(), any(), any()))
                .thenReturn(List.of(sample(1, "A"), sample(2, "B")));

        mvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("A"))
                .andExpect(jsonPath("$[0].status").value("open"))
                .andExpect(jsonPath("$[0].organizerUsername").value("ana"))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void getAll_emptyList_returns200AndEmptyArray() throws Exception {
        when(service.search(any(), any(), any(), any(), any())).thenReturn(List.of());

        mvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAll_passesFiltersToService() throws Exception {
        when(service.search(any(), any(), any(), any(), any())).thenReturn(List.of());

        mvc.perform(get("/api/v1/events")
                        .param("sportId", "1")
                        .param("city", "Zagreb")
                        .param("status", "open"))
                .andExpect(status().isOk());

        verify(service).search(eq((short) 1), eq("Zagreb"), isNull(), isNull(), eq("open"));
    }

    @Test
    void getAll_parsesFromAndToTimestamps() throws Exception {
        when(service.search(any(), any(), any(), any(), any())).thenReturn(List.of());

        mvc.perform(get("/api/v1/events")
                        .param("from", "2099-01-01T00:00:00Z")
                        .param("to", "2099-12-31T23:59:59Z"))
                .andExpect(status().isOk());

        verify(service).search(isNull(), isNull(),
                eq(OffsetDateTime.parse("2099-01-01T00:00:00Z")),
                eq(OffsetDateTime.parse("2099-12-31T23:59:59Z")), isNull());
    }

    @Test
    void getAll_invalidSportId_returns400() throws Exception {
        mvc.perform(get("/api/v1/events").param("sportId", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAll_invalidDate_returns400() throws Exception {
        mvc.perform(get("/api/v1/events").param("from", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /events/{id} ----------

    @Test
    void getOne_found_returnsEvent() throws Exception {
        when(service.findDetailById(5L)).thenReturn(Optional.of(sample(5, "Košarka")));

        mvc.perform(get("/api/v1/events/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Košarka"))
                .andExpect(jsonPath("$.participantCount").value(1));
    }

    @Test
    void getOne_notFound_returns404() throws Exception {
        when(service.findDetailById(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/events/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOne_nonNumericId_returns400() throws Exception {
        mvc.perform(get("/api/v1/events/abc"))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /users/me/events ----------

    @Test
    void myEvents_returnsJoinedEvents() throws Exception {
        when(service.findJoinedBy(eq(USER_ID), any())).thenReturn(List.of(sample(1, "A")));

        mvc.perform(get("/api/v1/users/me/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void myEvents_noJoinedEvents_returnsEmptyArray() throws Exception {
        when(service.findJoinedBy(eq(USER_ID), any())).thenReturn(List.of());

        mvc.perform(get("/api/v1/users/me/events").param("status", "open"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(service).findJoinedBy(USER_ID, "open");
    }

    // ---------- POST /events ----------

    @Test
    void create_valid_returns201WithLocation() throws Exception {
        Event created = new Event();
        created.setId(7L);
        when(service.create(any(), eq(USER_ID))).thenReturn(created);
        when(service.findDetailById(7L)).thenReturn(Optional.of(sample(7, "Petak nogomet")));

        mvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/events/7")))
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void create_emptyBody_returns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Naziv ne smije biti prazan"))
                .andExpect(jsonPath("$.errors.sportId").value("Sport je obavezan"))
                .andExpect(jsonPath("$.errors.venueId").value("Venue je obavezan"))
                .andExpect(jsonPath("$.errors.scheduledAt").value("Termin je obavezan"))
                .andExpect(jsonPath("$.errors.minAge").value("Minimalna dob je obavezna"))
                .andExpect(jsonPath("$.errors.maxParticipants").exists());
    }

    @Test
    void create_nameTooLong_returns400() throws Exception {
        String longName = "x".repeat(41);
        String body = VALID_CREATE_BODY.replace("Petak nogomet", longName);

        mvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Naziv smije imati najviše 40 znakova."));
    }

    @Test
    void create_scheduledInPast_returns400() throws Exception {
        String body = VALID_CREATE_BODY.replace("2099-01-01T10:00:00Z", "2000-01-01T10:00:00Z");

        mvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.scheduledAt").value("Termin mora biti sada ili u budućnosti."));
    }

    @Test
    void create_minAgeBelow13_returns400() throws Exception {
        String body = VALID_CREATE_BODY.replace("\"minAge\":18", "\"minAge\":12");

        mvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.minAge").exists());
    }

    @Test
    void create_malformedJson_returns400() throws Exception {
        mvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.body").value("Neispravan JSON zahtjev."));
    }

    @Test
    void create_businessValidationFailure_returns400WithErrors() throws Exception {
        when(service.create(any(), eq(USER_ID)))
                .thenThrow(new ApiValidationException(Map.of("sportId", "Sport ne postoji.")));

        mvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sportId").value("Sport ne postoji."));
    }

    // ---------- PATCH /events/{id} ----------

    @Test
    void update_notOrganizer_returns403() throws Exception {
        when(service.update(eq(3L), any(), eq(USER_ID)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Samo organizator može izvršiti ovu radnju."));

        mvc.perform(patch("/api/v1/events/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Novi naziv\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Samo organizator može izvršiti ovu radnju."));
    }

    @Test
    void update_blankName_returns400() throws Exception {
        mvc.perform(patch("/api/v1/events/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Naziv ne smije biti prazan."));
    }

    @Test
    void update_eventNotFound_returns404() throws Exception {
        when(service.update(eq(404L), any(), eq(USER_ID)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Događaj 404 ne postoji."));

        mvc.perform(patch("/api/v1/events/404")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Događaj 404 ne postoji."));
    }

    // ---------- POST /events/{id}/cancel ----------

    @Test
    void cancel_ok_returnsEvent() throws Exception {
        when(service.findDetailById(3L)).thenReturn(Optional.of(sample(3, "A")));

        mvc.perform(post("/api/v1/events/3/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));

        verify(service).cancel(3L, USER_ID);
    }

    @Test
    void cancel_alreadyCancelled_returns409() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Događaj je već otkazan."))
                .when(service).cancel(3L, USER_ID);

        mvc.perform(post("/api/v1/events/3/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Događaj je već otkazan."));
    }
}
