package hr.fer.letsdosports.user.domain;

import java.util.List;

public record UserSummary(long id, String username, String name, String profilePhotoUrl,
                          int age, City city, List<Sport> sports) {
    public UserSummary {
        sports = List.copyOf(sports);
    }

    public record City(int id, String name) {
    }

    public record Sport(int sportId, String sportName, int skillLevelId, String skillLevelName) {
    }
}
