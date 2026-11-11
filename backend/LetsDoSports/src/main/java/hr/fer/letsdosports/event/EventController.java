package hr.fer.letsdosports.event;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }

    @GetMapping
    public List<Event> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getOne(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Event create(@Valid @RequestBody CreateEventRequest req) {
        return service.create(req);
    }

    @GetMapping("/me")
    public List<Event> mine(@RequestHeader("X-User-Id") Long userId) {
        return service.findJoinedBy(userId);
    }

    @PatchMapping("/{eventId}")
    public Event update(@PathVariable Long eventId, @Valid @RequestBody UpdateEventRequest req) {
        return service.update(eventId, req, null);
    }

    @PatchMapping("/me/{eventId}")
    public Event updateOwn(@PathVariable Long eventId,
                           @RequestHeader("X-User-Id") Long userId,
                           @Valid @RequestBody UpdateEventRequest req) {
        return service.update(eventId, req, userId);
    }

    @DeleteMapping("/{eventId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long eventId) {
        service.cancel(eventId);
    }
}