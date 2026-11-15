package hr.fer.letsdosports.user.application;

import hr.fer.letsdosports.user.domain.UserSummary;
import hr.fer.letsdosports.user.domain.UserSearchFilter;
import hr.fer.letsdosports.user.domain.exception.InvalidUserSearchFilterException;
import hr.fer.letsdosports.user.domain.exception.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DefaultUserSearchService implements UserSearchService {
    private final UserSearchRepository repository;

    public DefaultUserSearchService(UserSearchRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<UserSummary> search(UserSearchFilter filter) {
        if (filter == null) {
            throw new InvalidUserSearchFilterException("Filtri nisu zadani.");
        }
        filter.validate();
        return repository.search(filter, today());
    }

    @Override
    public UserSummary findById(long id) {
        if (id < 1) {
            throw new InvalidUserSearchFilterException("ID igrača mora biti pozitivan.");
        }
        return repository.findById(id, today()).orElseThrow(UserNotFoundException::new);
    }

    private static LocalDate today() {
        var now = new Timestamp(System.currentTimeMillis());
        return now.toInstant().atZone(ZoneId.of("Europe/Zagreb")).toLocalDate();
    }
}
