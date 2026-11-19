package hr.fer.letsdosports.eventmember;

import hr.fer.letsdosports.auth.CurrentUserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EventMemberControllerTest {

    private static final long EVENT_ID = 10L;
    private static final long USER_ID = 1L;
    private static final String URL = "/events/{eventId}/members";

    @Mock
    private EventMemberService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders
                .standaloneSetup(new EventMemberController(service))
                .setCustomArgumentResolvers(new HeaderUserIdResolver())
                .build();
    }

    // ---------- GET /events/{eventId}/members ----------

    @Test
    void getMembers_returns200WithMembers() throws Exception {
        var organizer = new EventMemberResponse(
                1L, "ana", "Ana Horvat",
                OffsetDateTime.parse("2026-11-01T10:00:00+01:00"),  "https://example.com/photos/ana.jpg",
                List.of(new EventMemberResponse.MemberSport(1L, 2L)),
                true);
        var member = new EventMemberResponse(
                2L, "marko", "Marko Kovač",
                OffsetDateTime.parse("2026-11-02T12:30:00+01:00"),  "https://example.com/photos/ana.jpg",
                List.of(),
                false);
        when(service.listMembers(EVENT_ID)).thenReturn(List.of(organizer, member));

        mvc.perform(get(URL, EVENT_ID).header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].username").value("ana"))
                .andExpect(jsonPath("$[0].name").value("Ana Horvat"))
                .andExpect(jsonPath("$[0].joinedAt").exists())
                .andExpect(jsonPath("$[0].organizer").value(true))
                .andExpect(jsonPath("$[0].sports[0].sportId").value(1))
                .andExpect(jsonPath("$[0].sports[0].skillLevelId").value(2))
                .andExpect(jsonPath("$[1].organizer").value(false))
                .andExpect(jsonPath("$[1].sports").isEmpty());
    }

    @Test
    void getMembers_noMembers_returns200AndEmptyArray() throws Exception {
        when(service.listMembers(EVENT_ID)).thenReturn(List.of());

        mvc.perform(get(URL, EVENT_ID).header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getMembers_unknownEvent_returns404() throws Exception {
        when(service.listMembers(EVENT_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mvc.perform(get(URL, EVENT_ID).header("X-User-Id", USER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMembers_invalidEventId_returns400() throws Exception {
        mvc.perform(get(URL, "abc").header("X-User-Id", USER_ID))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    // ---------- POST /events/{eventId}/members (join) ----------

    @Test
    void join_returns201AndCallsServiceWithCurrentUser() throws Exception {
        mvc.perform(post(URL, EVENT_ID).header("X-User-Id", USER_ID))
                .andExpect(status().isCreated());

        verify(service).join(EVENT_ID, USER_ID);
    }

    @Test
    void join_unknownEvent_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(service).join(EVENT_ID, USER_ID);

        mvc.perform(post(URL, EVENT_ID).header("X-User-Id", USER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void join_alreadyMember_returns409() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "already a member"))
                .when(service).join(EVENT_ID, USER_ID);

        mvc.perform(post(URL, EVENT_ID).header("X-User-Id", USER_ID))
                .andExpect(status().isConflict());
    }

    @Test
    void join_fullEvent_returns409() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "event is full"))
                .when(service).join(EVENT_ID, USER_ID);

        mvc.perform(post(URL, EVENT_ID).header("X-User-Id", USER_ID))
                .andExpect(status().isConflict());
    }

    @Test
    void join_cancelledEvent_returns409() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "event is cancelled"))
                .when(service).join(EVENT_ID, USER_ID);

        mvc.perform(post(URL, EVENT_ID).header("X-User-Id", USER_ID))
                .andExpect(status().isConflict());
    }

    // ---------- DELETE /events/{eventId}/members/me (leave) ----------

    @Test
    void leave_returns204AndCallsServiceWithCurrentUser() throws Exception {
        mvc.perform(delete(URL + "/me", EVENT_ID).header("X-User-Id", USER_ID))
                .andExpect(status().isNoContent());

        verify(service).leave(EVENT_ID, USER_ID);
    }

    @Test
    void leave_organizer_isRejected() throws Exception {
        // spec §6: organizer must cancel the event instead of leaving
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "organizer cannot leave"))
                .when(service).leave(EVENT_ID, USER_ID);

        mvc.perform(delete(URL + "/me", EVENT_ID).header("X-User-Id", USER_ID))
                .andExpect(status().isConflict());
    }

    @Test
    void leave_notAParticipant_returns403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(service).leave(EVENT_ID, USER_ID);

        mvc.perform(delete(URL + "/me", EVENT_ID).header("X-User-Id", USER_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void leave_unknownEvent_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(service).leave(EVENT_ID, USER_ID);

        mvc.perform(delete(URL + "/me", EVENT_ID).header("X-User-Id", USER_ID))
                .andExpect(status().isNotFound());
    }

    private static class HeaderUserIdResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(CurrentUserId.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
                                      NativeWebRequest request, WebDataBinderFactory binderFactory) {
            return Long.valueOf(request.getHeader("X-User-Id"));
        }
    }
}