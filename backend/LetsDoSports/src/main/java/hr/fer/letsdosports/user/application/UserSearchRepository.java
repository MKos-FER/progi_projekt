package hr.fer.letsdosports.user.application;

import hr.fer.letsdosports.user.domain.UserSummary;
import hr.fer.letsdosports.user.domain.UserSearchFilter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserSearchRepository {

    List<UserSummary> search(UserSearchFilter filter, LocalDate today);

    Optional<UserSummary> findById(long id, LocalDate today);
}
