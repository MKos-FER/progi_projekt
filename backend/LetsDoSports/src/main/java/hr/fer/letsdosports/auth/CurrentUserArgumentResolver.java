package hr.fer.letsdosports.auth;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {
    private final RegistrationService registration;

    public CurrentUserArgumentResolver(RegistrationService registration) { this.registration = registration; }

    @Override
    public boolean supportsParameter(MethodParameter p) {
        return p.hasParameterAnnotation(CurrentUserId.class) && Long.class.equals(p.getParameterType());
    }

    // Korisnik se određuje iz OAuth prijave (sesije) preko povezanog zapisa u oauth_accounts.
    @Override
    public Object resolveArgument(MethodParameter p, ModelAndViewContainer mav,
                                  NativeWebRequest req, WebDataBinderFactory binder) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof OAuth2AuthenticationToken token)) throw unauthorized();
        return registration.findLinkedUserId(token).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Registracija nije dovršena."));
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Prijava je obavezna.");
    }
}