package hr.fer.letsdosports.eventmember;

import hr.fer.letsdosports.event.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events/{eventId}/members")
public class EventMemberController {

    private final EventMemberService service;

    public EventMemberController(EventMemberService service) {
        this.service = service;
    }

    @GetMapping
    public List<EventMemberResponse> getMembers(@PathVariable Long eventId) {
        return service.listMembers(eventId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void join(@PathVariable Long eventId, @RequestHeader("X-User-Id") Long userId) {
        service.join(eventId, userId);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long eventId, @RequestHeader("X-User-Id") Long userId) {
        service.removeMember(eventId, userId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void kick(@PathVariable Long eventId, @PathVariable Long userId) {
        service.removeMember(eventId, userId);
    }
}