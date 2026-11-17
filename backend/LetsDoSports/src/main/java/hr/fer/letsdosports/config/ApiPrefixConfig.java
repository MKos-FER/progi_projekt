package hr.fer.letsdosports.config;

import hr.fer.letsdosports.auth.CurrentUserArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class ApiPrefixConfig implements WebMvcConfigurer {
    private final CurrentUserArgumentResolver currentUser;

    public ApiPrefixConfig(CurrentUserArgumentResolver currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api/v1",
                HandlerTypePredicate.forAnnotation(RestController.class));
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUser);
    }
}