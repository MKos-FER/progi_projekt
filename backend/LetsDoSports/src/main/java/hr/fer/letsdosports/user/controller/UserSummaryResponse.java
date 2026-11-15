package hr.fer.letsdosports.user.controller;

import hr.fer.letsdosports.user.domain.UserSummary;

import java.util.List;

public record UserSummaryResponse(long id, String username, String name, String profilePhotoUrl,
                                  int age, CityResponse city, List<SportResponse> sports) {
    public static UserSummaryResponse from(UserSummary user) {
        return new UserSummaryResponse(user.id(), user.username(), user.name(),
                user.profilePhotoUrl(),
                user.age(), new CityResponse(user.city().id(), user.city().name()),
                user.sports().stream()
                        .map(sport -> new SportResponse(sport.sportId(), sport.sportName(),
                                sport.skillLevelId(), sport.skillLevelName())).toList());
    }

    public record CityResponse(int id, String name) {
    }

    public record SportResponse(int sportId, String sportName, int skillLevelId,
                                String skillLevelName) {
    }
}
