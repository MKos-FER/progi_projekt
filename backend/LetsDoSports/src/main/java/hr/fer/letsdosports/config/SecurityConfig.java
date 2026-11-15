package hr.fer.letsdosports.config;

import hr.fer.letsdosports.auth.RegistrationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            @Value("${app.frontend-url}") String frontendUrl,
                                            RegistrationService registration) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // AuthFlow rute iz api-endpoints
                        .requestMatchers(
                                "/oauth2/**",
                                "/oauth2/login/**",
                                "/error"
                        ).permitAll()

                        // Public rute iz api-endpoints
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/events",
                                "/api/v1/events/{eventId}",
                                "/api/v1/events/{eventId}/comments",
                                "/api/v1/sports",
                                "/api/v1/sports/{sportId}",
                                "/api/v1/venues",
                                "/api/v1/venues/{venueId}",
                                "/api/v1/auth/csrf"
                        ).permitAll()

                        // Authenticated rute iz api-endpoints
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())

                // Bez preusmjeravanja na Google; neprijavljeni dobivaju 401
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .oauth2Login(o -> o
                        .redirectionEndpoint(endpoint ->
                                endpoint.baseUri("/oauth2/login/code/*")
                        )
                        .loginPage(frontendUrl + "/")   // isključuje Springovu generiranu /login stranicu

                        // Novi Google račun ide na stranicu za registraciju, postojeći na početnu
                        .successHandler((req, res, auth) -> res.sendRedirect(
                                registration.findLinkedUserId((OAuth2AuthenticationToken) auth).isPresent()
                                        ? frontendUrl + "/"
                                        : frontendUrl + "/register")
                        )
                        .failureUrl(frontendUrl + "/login?loginError"))
                .logout(l -> l
                        .logoutUrl("/api/v1/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))

                // Session cookie => CSRF zaštita; frontend šalje XSRF-TOKEN cookie u X-XSRF-TOKEN header
                .csrf(c -> c.spa());
        return http.build();
    }
}
