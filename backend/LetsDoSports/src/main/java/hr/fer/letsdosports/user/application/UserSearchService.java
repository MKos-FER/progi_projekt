package hr.fer.letsdosports.user.application;

import hr.fer.letsdosports.user.domain.UserSummary;
import hr.fer.letsdosports.user.domain.UserSearchFilter;

import java.util.List;

public interface UserSearchService {

    List<UserSummary> search(UserSearchFilter filter);

    UserSummary findById(long id);

}
