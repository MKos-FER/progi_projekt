package hr.fer.letsdosports.event;

import hr.fer.letsdosports.auth.CurrentUserId;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {
    private final EventService service;
    public EventController(EventService service) { this.service = service; }

    @GetMapping   // Public
    public List<EventResponse> getAll(
            @RequestParam(required = false) Short sportId,
            @RequestParam(required = false) String city,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String status) {
        return service.search(sportId, city, from, to, status);
    }

    @GetMapping("/{id}")   // Public
    public ResponseEntity<EventResponse> getOne(@PathVariable Long id) {
        return service.findDetailById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping   // Authenticated
    public ResponseEntity<EventResponse> create(@
                                                            CurrentUserId Long userId,
                                                @Valid @RequestBody CreateEventRequest req) {
        Event e = service.create(req, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(e.getId()).toUri();
        return ResponseEntity.created(location).body(service.findDetailById(e.getId()).orElseThrow());
    }

    @PatchMapping("/{eventId}")   // Organizer
    public EventResponse update(@PathVariable Long eventId, @CurrentUserId Long userId,
                                @Valid @RequestBody UpdateEventRequest req) {
        service.update(eventId, req, userId);
        return service.findDetailById(eventId).orElseThrow();
    }

    @PostMapping("/{eventId}/cancel")   // Organizer
    public EventResponse cancel(@PathVariable Long eventId, @CurrentUserId Long userId) {
        service.cancel(eventId, userId);
        return service.findDetailById(eventId).orElseThrow();
    }
}