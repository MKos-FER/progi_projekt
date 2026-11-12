package hr.fer.letsdosports.eventmember;

import hr.fer.letsdosports.event.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import hr.fer.letsdosports.auth.CurrentUserId;

@RestController
@RequestMapping("/events/{eventId}/members")
public class EventMemberController {

    private final EventMemberService service;

    public EventMemberController(EventMemberService service) {
        this.service = service;
    }

    @GetMapping
    public List<EventMemberResponse> getMembers(@PathVariable Long eventId, @CurrentUserId Long userId) {
        return service.listMembers(eventId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void join(@PathVariable Long eventId, @CurrentUserId Long userId) {
        service.join(eventId, userId);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long eventId, @CurrentUserId Long userId) {
        service.leave(eventId, userId);
    }


}