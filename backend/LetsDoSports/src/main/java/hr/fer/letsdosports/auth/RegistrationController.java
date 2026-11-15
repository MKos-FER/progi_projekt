package hr.fer.letsdosports.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class RegistrationController {
    private final RegistrationService service;

    public RegistrationController(RegistrationService service) { this.service = service; }

    // Dovršetak registracije nakon prve Google prijave (trenutno authenticated ali korisnik još ne postoji)
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> register(OAuth2AuthenticationToken token, @Valid @RequestBody RegisterRequest req) {
        return Map.of("id", service.register(token, req));
    }
}
