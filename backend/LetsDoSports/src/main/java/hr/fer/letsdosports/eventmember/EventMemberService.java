package hr.fer.letsdosports.eventmember;

import hr.fer.letsdosports.event.EventRepository;
import hr.fer.letsdosports.event.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class EventMemberService {
    private final EventService eventService;
    private final EventRepository events;
    private final EventMemberRepository members;

    public EventMemberService(EventService eventService, EventRepository events,
                              EventMemberRepository members) {
        this.eventService = eventService;
        this.events = events;
        this.members = members;
    }

    public List<EventMemberResponse> listMembers(Long eventId) {
        if (eventService.findById(eventId).isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event " + eventId + " not found.");
        return members.listMembers(eventId);
    }

    @Transactional
    public void join(Long eventId, Long userId) {
        eventService.lockEvent(eventId);
        eventService.requireUser(userId);
        if (members.existsByEventIdAndUserId(eventId, userId))
            throw eventService.conflict("Već si prijavljen na ovaj događaj.");
        switch (eventService.statusOf(eventId)) {
            case "open" -> { }
            case "full" -> throw eventService.conflict("Događaj je popunjen.");
            case "cancelled" -> throw eventService.conflict("Događaj je otkazan.");
            default -> throw eventService.conflict("Događaj je već počeo ili završio.");
        }
        members.insert(eventId, userId);
    }

    @Transactional
    public void removeMember(Long eventId, Long userId) {
        eventService.lockEvent(eventId);
        if (!eventService.statusOf(eventId).matches("open|full"))
            throw eventService.conflict("Događaj je već počeo, završio ili je otkazan.");
        if (members.deleteByEventIdAndUserId(eventId, userId) == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Korisnik nije član događaja.");

        if (events.findCreatedById(eventId).equals(userId)) {
            Optional<Long> next = members.findNextUserId(eventId);
            if (next.isPresent()) events.setCreatedBy(eventId, next.get());
            else events.cancel(eventId);
        }
    }
}