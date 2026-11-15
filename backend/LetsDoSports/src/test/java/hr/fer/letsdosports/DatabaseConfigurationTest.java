package hr.fer.letsdosports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConfigurationTest {

    @TempDir
    Path directory;

    @Test
    void loadsDatabaseSettingsFromLocalEnvFile() throws IOException {
        Path envFile = Files.writeString(directory.resolve(".env"), """
                POSTGRES_DB=test_sports
                APP_DB_USER=test_app
                APP_DB_PASSWORD=test_password
                """);

        runner("file:" + envFile.toUri().getPath() + "[.properties]").run(context -> {
            assertThat(context).hasNotFailed();
            var environment = context.getEnvironment();
            assertThat(environment.getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:postgresql://localhost:5432/test_sports");
            assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("test_app");
            assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("test_password");
        });
    }

    @Test
    void explicitSettingsWorkWithoutLocalEnvFile() {
        runner("optional:file:" + directory.resolve("missing.env").toUri().getPath() + "[.properties]")
                .withSystemProperties(
                        "LDS_DB_URL=jdbc:postgresql://database:5432/remote_sports",
                        "LDS_DB_USER=remote_app",
                        "LDS_DB_PASSWORD=remote_password")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var environment = context.getEnvironment();
                    assertThat(environment.getProperty("spring.datasource.url"))
                            .isEqualTo("jdbc:postgresql://database:5432/remote_sports");
                    assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("remote_app");
                    assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("remote_password");
                });
    }

    private ApplicationContextRunner runner(String location) {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.config.location=classpath:/application.properties",
                        "spring.config.import=" + location);
    }
}
