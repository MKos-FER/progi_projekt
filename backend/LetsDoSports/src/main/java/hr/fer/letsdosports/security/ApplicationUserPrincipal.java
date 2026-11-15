package hr.fer.letsdosports.security;

import java.io.Serializable;
import java.security.Principal;

public record ApplicationUserPrincipal(long localUserId) implements Principal, Serializable {
    public ApplicationUserPrincipal {
        if (localUserId < 1) {
            throw new IllegalArgumentException("Local user ID must be positive.");
        }
    }

    @Override
    public String getName() {
        return Long.toString(localUserId);
    }
}
