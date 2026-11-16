package hr.fer.letsdosports.sport;

public record SportDto(
        Short sportId,
        String sportName,
        Short skillLevelId,
        String skillLevelName
) {}
