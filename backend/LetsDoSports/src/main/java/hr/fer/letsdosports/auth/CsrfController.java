package hr.fer.letsdosports.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class CsrfController {

    // Osigurava XSRF-TOKEN cookie; frontend njegovu vrijednost šalje kao X-XSRF-TOKEN header.
    @GetMapping("/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void csrf(CsrfToken token) {
        token.getToken();   // učitava token i postavlja cookie
    }
}
