package hr.fer.letsdosports.event;

import hr.fer.letsdosports.eventmember.EventMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


class EventServiceTest {

    private EventRepository events;
    private EventMemberRepository members;
    private EventService service;   // spy

    @BeforeEach
    void setUp() {
        events = mock(EventRepository.class);
        members = mock(EventMemberRepository.class);
        service = spy(new EventService(events, members, mock(JdbcClient.class)));
        doNothing().when(service).lockEvent(any());
    }

    private static Event eventOwnedBy(long organizerId) {
        Event e = new Event();
        e.setId(1L);
        e.setCreatedById(organizerId);
        e.setName("Stari naziv");
        return e;
    }

    // ---------- search / retrieval ----------

    @Test
    void search_noEvents_returnsEmptyList() {
        when(events.search(any(), any(), any(), any(), any())).thenReturn(List.of());

        assertThat(service.search(null, null, null, null, null)).isEmpty();
    }

    @Test
    void search_blankStatus_isNormalisedToNull() {
        service.search(null, null, null, null, "   ");

        verify(events).search(null, null, null, null, null);
    }

    @Test
    void search_status_isLowercased() {
        service.search((short) 1, "Zagreb", null, null, "OPEN");

        verify(events).search((short) 1, "Zagreb", null, null, "open");
    }

    @Test
    void findDetailById_missing_returnsEmptyOptional() {
        when(events.findDetailById(99L)).thenReturn(Optional.empty());

        assertThat(service.findDetailById(99L)).isEmpty();
    }

    @Test
    void findJoinedBy_userWithoutEvents_returnsEmptyList() {
        when(events.findJoinedBy(5L, null)).thenReturn(List.of());

        assertThat(service.findJoinedBy(5L, null)).isEmpty();
    }

    @Test
    void findJoinedBy_blankStatus_isNormalisedToNull() {
        service.findJoinedBy(5L, "");

        verify(events).findJoinedBy(5L, null);
    }

    // ---------- cancel ----------

    @Test
    void cancel_eventDoesNotExist_returns404() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Događaj 1 ne postoji."))
                .when(service).lockEvent(1L);

        assertThatThrownBy(() -> service.cancel(1L, 7L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(events, never()).cancel(any());
    }

    @Test
    void cancel_byNonOrganizer_returns403() {
        when(events.findCreatedById(1L)).thenReturn(7L);

        assertThatThrownBy(() -> service.cancel(1L, 8L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(events, never()).cancel(any());
    }

    @Test
    void cancel_alreadyCancelled_returns409() {
        when(events.findCreatedById(1L)).thenReturn(7L);
        doReturn("cancelled").when(service).statusOf(1L);

        assertThatThrownBy(() -> service.cancel(1L, 7L))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getReason()).isEqualTo("Događaj je već otkazan.");
                });
    }

    @Test
    void cancel_finished_returns409() {
        when(events.findCreatedById(1L)).thenReturn(7L);
        doReturn("finished").when(service).statusOf(1L);

        assertThatThrownBy(() -> service.cancel(1L, 7L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void cancel_openEventByOrganizer_cancelsIt() {
        when(events.findCreatedById(1L)).thenReturn(7L);
        doReturn("open").when(service).statusOf(1L);

        service.cancel(1L, 7L);

        verify(events).cancel(1L);
    }

    // ---------- update ----------

    @Test
    void update_byNonOrganizer_returns403() {
        when(events.findCreatedById(1L)).thenReturn(7L);
        var req = new UpdateEventRequest("Novo", null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.update(1L, req, 8L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(events, never()).update(any());
    }

    @Test
    void update_cancelledEvent_returns409() {
        when(events.findCreatedById(1L)).thenReturn(7L);
        when(events.findById(1L)).thenReturn(Optional.of(eventOwnedBy(7L)));
        doReturn("cancelled").when(service).statusOf(1L);
        var req = new UpdateEventRequest("Novo", null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.update(1L, req, 7L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void update_capacityBelowCurrentMembers_throwsValidationError() {
        when(events.findCreatedById(1L)).thenReturn(7L);
        when(events.findById(1L)).thenReturn(Optional.of(eventOwnedBy(7L)));
        doReturn("open").when(service).statusOf(1L);
        when(members.countByEventId(1L)).thenReturn(5L);
        var req = new UpdateEventRequest(null, null, null, null, null, null, 3, null, null, null);

        assertThatThrownBy(() -> service.update(1L, req, 7L))
                .isInstanceOfSatisfying(ApiValidationException.class,
                        ex -> assertThat(ex.getErrors()).containsKey("maxParticipants"));
        verify(events, never()).update(any());
    }

    @Test
    void update_onlyProvidedFieldsAreChanged() {
        Event existing = eventOwnedBy(7L);
        existing.setDescription("stari opis");
        when(events.findCreatedById(1L)).thenReturn(7L);
        when(events.findById(1L)).thenReturn(Optional.of(existing));
        doReturn("open").when(service).statusOf(1L);
        when(events.update(any())).thenAnswer(inv -> inv.getArgument(0));
        var req = new UpdateEventRequest("Novi naziv", null, null, null, null, null, null, null, null, null);

        Event result = service.update(1L, req, 7L);

        assertThat(result.getName()).isEqualTo("Novi naziv");
        assertThat(result.getDescription()).isEqualTo("stari opis");
    }
}
