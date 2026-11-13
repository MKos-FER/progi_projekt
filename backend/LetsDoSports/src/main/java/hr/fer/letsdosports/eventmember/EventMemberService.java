package hr.fer.letsdosports.eventmember;

import hr.fer.letsdosports.event.EventRepository;
import hr.fer.letsdosports.event.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import hr.fer.letsdosports.event.Event;

import java.util.List;

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
        if (members.existsByEventIdAndUserId(eventId, userId))
            throw eventService.conflict("Već si prijavljen na ovaj događaj.");
        switch (eventService.statusOf(eventId)) {
            case "open" -> { }
            case "full" -> throw eventService.conflict("Događaj je popunjen.");
            case "cancelled" -> throw eventService.conflict("Događaj je otkazan.");
            default -> throw eventService.conflict("Događaj je već počeo ili završio.");
        }
        requireEligible(eventId, userId);
        members.insert(eventId, userId);
    }

    private void requireEligible(Long eventId, Long userId) {
        Event e = events.findById(eventId).orElseThrow();

        if (e.getMinAge() != null || e.getMaxAge() != null) {
            int age = members.findAgeYears(userId).orElseThrow(() ->
                    eventService.conflict("Dovrši profil (datum rođenja) prije prijave."));
            if (e.getMinAge() != null && age < e.getMinAge())
                throw eventService.conflict("Događaj je za osobe od " + e.getMinAge() + " godina.");
            if (e.getMaxAge() != null && age > e.getMaxAge())
                throw eventService.conflict("Događaj je za osobe do " + e.getMaxAge() + " godina.");
        }

        if (e.getMinSkillLevelId() != null) {
            int required = members.findSkillRank(e.getMinSkillLevelId()).orElseThrow();
            int mine = members.findUserSkillRank(userId, e.getSportId()).orElseThrow(() ->
                    eventService.conflict("Navedi svoju razinu iskustva za ovaj sport prije prijave."));
            if (mine < required)
                throw eventService.conflict("Tvoja razina iskustva je preniska za ovaj događaj.");
        }
    }

    @Transactional
    public void leave(Long eventId, Long userId) {
        eventService.lockEvent(eventId);
        if (events.findCreatedById(eventId).equals(userId))
            throw eventService.conflict("Organizator ne može napustiti događaj, otkaži ga.");
        if (!eventService.statusOf(eventId).matches("open|full"))
            throw eventService.conflict("Događaj je već počeo, završio ili je otkazan.");
        if (members.deleteByEventIdAndUserId(eventId, userId) == 0)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nisi sudionik ovog događaja.");
    }
}