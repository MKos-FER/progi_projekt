package hr.fer.letsdosports.event;

import hr.fer.letsdosports.auth.CurrentUserId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users/me/events")
public class MyEventsController {
    private final EventService service;
    public MyEventsController(EventService service) { this.service = service; }

    @GetMapping   // Authenticated
    public List<EventResponse> mine(@CurrentUserId Long userId,
                                    @RequestParam(required = false) String status) {
        return service.findJoinedBy(userId, status);
    }
}
