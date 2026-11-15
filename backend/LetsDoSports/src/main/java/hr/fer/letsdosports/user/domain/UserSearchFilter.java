package hr.fer.letsdosports.user.domain;

import hr.fer.letsdosports.user.domain.exception.InvalidUserSearchFilterException;

public record UserSearchFilter(Integer sportId, Integer cityId, Integer ageMin,
                               Integer ageMax, Integer skillLevelId) {
    public void validate() {
        validateLookupId(sportId);
        validateLookupId(cityId);
        validateLookupId(skillLevelId);
        if (skillLevelId != null && sportId == null) {
            throw new InvalidUserSearchFilterException("skillLevelId zahtijeva sportId.");
        }
        if ((ageMin != null && ageMin < 0) || (ageMax != null && ageMax < 0)
                || (ageMin != null && ageMax != null && ageMin > ageMax)) {
            throw new InvalidUserSearchFilterException(
                    "Dobne granice moraju biti nenegativne i ageMin <= ageMax.");
        }
    }

    private static void validateLookupId(Integer id) {
        if (id != null && (id < 1 || id > Short.MAX_VALUE)) {
            throw new InvalidUserSearchFilterException("ID šifrarnika mora biti između 1 i 32767.");
        }
    }
}
