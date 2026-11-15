package hr.fer.letsdosports.user;

import hr.fer.letsdosports.user.application.DefaultUserSearchService;
import hr.fer.letsdosports.user.application.UserSearchRepository;
import hr.fer.letsdosports.user.domain.UserSearchFilter;
import hr.fer.letsdosports.user.domain.exception.InvalidUserSearchFilterException;
import hr.fer.letsdosports.user.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DefaultUserSearchServiceTest {
    private final UserSearchRepository repository = mock(UserSearchRepository.class);
    private final DefaultUserSearchService service = new DefaultUserSearchService(repository);

    @Test
    void forwardsCombinedFiltersAndReturnsEmptyResults() {
        var filter = new UserSearchFilter(1, 2, 18, 30, 3);
        when(repository.search(eq(filter), any(LocalDate.class))).thenReturn(List.of());
        assertThat(service.search(filter)).isEmpty();
        verify(repository).search(eq(filter), any(LocalDate.class));
    }

    @ParameterizedTest
    @MethodSource("invalidFilters")
    void rejectsInvalidFiltersBeforeReadingDatabase(UserSearchFilter filter) {
        assertThatThrownBy(() -> service.search(filter)).isInstanceOf(InvalidUserSearchFilterException.class);
        verifyNoInteractions(repository);
    }

    static Stream<UserSearchFilter> invalidFilters() {
        return Stream.of(new UserSearchFilter(null, null, null, null, 1),
                new UserSearchFilter(0, null, null, null, null),
                new UserSearchFilter(null, 32768, null, null, null),
                new UserSearchFilter(1, null, null, null, -1),
                new UserSearchFilter(null, null, -1, null, null),
                new UserSearchFilter(null, null, null, -1, null),
                new UserSearchFilter(null, null, 30, 18, null));
    }

    @Test
    void allowsZeroAgeAndEqualBounds() {
        var filter = new UserSearchFilter(null, null, 0, 0, null);
        when(repository.search(eq(filter), any(LocalDate.class))).thenReturn(List.of());
        assertThat(service.search(filter)).isEmpty();
    }

    @Test
    void rejectsNonPositiveUserId() {
        assertThatThrownBy(() -> service.findById(0)).isInstanceOf(InvalidUserSearchFilterException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void reportsMissingUser() {
        when(repository.findById(eq(123L), any(LocalDate.class))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(123)).isInstanceOf(UserNotFoundException.class);
    }
}
