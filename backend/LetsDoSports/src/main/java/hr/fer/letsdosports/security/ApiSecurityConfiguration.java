package hr.fer.letsdosports.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
public class ApiSecurityConfiguration {
    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        // CSRF protection stays enabled by default for requests that change data.
        return http
                // Use the CORS bean below to allow requests from the configured frontend origins.
                .cors(cors -> {
                })
                // OAuth login is planned separately; do not expose form login or HTTP Basic.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // Disable the default /logout endpoint until application logout is implemented.
                .logout(AbstractHttpConfigurer::disable)
                // API clients receive errors directly; no request needs saving for a login redirect.
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/v1/users", "/api/v1/users/*")
                        .access((authentication, context) -> {
                            var auth = authentication.get();
                            // Require a local application identity, not just any authenticated principal.
                            if (!auth.isAuthenticated() || !(auth.getPrincipal() instanceof ApplicationUserPrincipal)) {
                                // Treat a missing local identity as unauthenticated (401).
                                throw new InsufficientAuthenticationException(
                                        "A local user session is required.");
                            }
                            return new AuthorizationDecision(true);
                        })
                        // Keep all other routes closed until access is explicitly configured.
                        .anyRequest().denyAll())
                // Security errors occur before controllers, so return the API JSON format here.
                .exceptionHandling(errors -> errors
                        // 401: the request does not have an accepted authenticated identity.
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, 401, "UNAUTHENTICATED",
                                        "Potrebna je prijava."))
                        // 403: access is forbidden, including requests rejected by CSRF protection.
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, 403, "ACCESS_DENIED",
                                        "Pristup nije dopušten.")))
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${lds.cors.allowed-origins}") String origins) {
        var configuration = new CorsConfiguration();
        // Read comma-separated origins from configuration; trim spaces and ignore empty entries.
        configuration.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim)
                .filter(origin -> !origin.isEmpty()).toList());
        // Allow credentialed requests (session cookies) from those explicit origins.
        configuration.setAllowCredentials(true);
        // Allow reads and writes; OPTIONS supports browser preflight requests.
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        // Permit JSON content and CSRF token headers; listing headers does not validate a token.
        configuration.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN", "X-XSRF-TOKEN"));
        var source = new UrlBasedCorsConfigurationSource();
        // Apply these cross-origin rules to API paths only.
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private static void writeError(HttpServletResponse response, int status, String code,
                                   String message)
            throws IOException {
        // Both handlers use the same {code,message} response with fixed messages above.
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
