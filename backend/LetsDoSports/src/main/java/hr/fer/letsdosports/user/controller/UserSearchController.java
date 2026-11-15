package hr.fer.letsdosports.user.controller;

import hr.fer.letsdosports.user.application.UserSearchService;
import hr.fer.letsdosports.user.domain.UserSearchFilter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserSearchController {

    private final UserSearchService service;

    public UserSearchController(UserSearchService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserSummaryResponse> search(
            @RequestParam(required = false) @Min(1) @Max(32767) Integer sportId,
            @RequestParam(required = false) @Min(1) @Max(32767) Integer cityId,
            @RequestParam(required = false) @Min(0) Integer ageMin,
            @RequestParam(required = false) @Min(0) Integer ageMax,
            @RequestParam(required = false) @Min(1) @Max(32767) Integer skillLevelId) {
        return service.search(new UserSearchFilter(sportId, cityId, ageMin, ageMax, skillLevelId))
                .stream().map(UserSummaryResponse::from).toList();
    }

    @GetMapping("/{userId}")
    public UserSummaryResponse findById(@PathVariable @Positive long userId) {
        return UserSummaryResponse.from(service.findById(userId));
    }
}
