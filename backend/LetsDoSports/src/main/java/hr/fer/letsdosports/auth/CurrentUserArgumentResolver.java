package hr.fer.letsdosports.auth;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {
    private final JdbcClient jdbc;

    public CurrentUserArgumentResolver(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override
    public boolean supportsParameter(MethodParameter p) {
        return p.hasParameterAnnotation(CurrentUserId.class) && Long.class.equals(p.getParameterType());
    }

    // DEV ONLY: replace the body with SecurityContext/OAuth principal lookup once auth exists.
    @Override
    public Object resolveArgument(MethodParameter p, ModelAndViewContainer mav,
                                  NativeWebRequest req, WebDataBinderFactory binder) {
        String header = req.getHeader("X-User-Id");
        if (header == null) throw unauthorized();
        long id;
        try { id = Long.parseLong(header.trim()); } catch (NumberFormatException e) { throw unauthorized(); }
        boolean exists = jdbc.sql("SELECT EXISTS(SELECT 1 FROM users WHERE id = :id)")
                .param("id", id).query(Boolean.class).single();
        if (!exists) throw unauthorized();
        return id;
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Prijava je obavezna.");
    }
}