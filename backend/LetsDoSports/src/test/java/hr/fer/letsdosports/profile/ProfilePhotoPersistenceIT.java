package hr.fer.letsdosports.profile;

import hr.fer.letsdosports.LetsDoSportsApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.ByteArrayOutputStream;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP, filesystem and PostgreSQL test. Use a disposable database migrated through V5. */
@EnabledIfEnvironmentVariable(named = "PROFILE_TEST_JDBC_URL", matches = ".+")
class ProfilePhotoPersistenceIT {
    @TempDir Path directory;

    @Test void photoSurvivesApplicationRestartAndCanBeReplaced() throws Exception {
        long userId;
        String firstUrl;
        byte[] original;
        try (var app = start()) {
            userId = app.getBean(JdbcClient.class).sql("""
                    INSERT INTO users(username, name, gender, date_of_birth, city_id)
                    SELECT :username, 'Photo integration test', 'Other', DATE '2000-01-01', id
                    FROM sif_city ORDER BY id LIMIT 1 RETURNING id
                    """).param("username", "photo-it-" + UUID.randomUUID()).query(Long.class).single();
            var browser = browser();
            String base = base(app);
            var empty = get(browser, base + "/api/v1/users/me/photo", userId);
            assertEquals(200, empty.statusCode());
            assertTrue(empty.body().contains("\"profilePhotoUrl\":null"));
            var uploaded = upload(browser, base, userId, empty.body(), PhotoStorageTest.photo("png").getBytes());
            assertEquals(200, uploaded.statusCode(), uploaded.body());
            firstUrl = field(uploaded.body(), "profilePhotoUrl");
            original = browser.send(request(base + firstUrl, userId).GET().build(), HttpResponse.BodyHandlers.ofByteArray()).body();
            assertTrue(original.length > 0);
        }
        // Close the entire Spring context/server, then start a new one with the same storage and DB.
        try (var app = start()) {
            var browser = browser();
            String base = base(app);
            var current = get(browser, base + "/api/v1/users/me/photo", userId);
            assertEquals(firstUrl, field(current.body(), "profilePhotoUrl"));
            var image = browser.send(request(base + firstUrl, userId).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, image.statusCode());
            assertArrayEquals(original, image.body());
            var invalid = upload(browser, base, userId, current.body(), "fake png".getBytes(StandardCharsets.UTF_8));
            assertEquals(400, invalid.statusCode());
            assertEquals(firstUrl, app.getBean(ProfilePhotoService.class).current(userId));
            var replaced = upload(browser, base, userId, current.body(), PhotoStorageTest.photo("png").getBytes());
            assertEquals(200, replaced.statusCode(), replaced.body());
            String secondUrl = field(replaced.body(), "profilePhotoUrl");
            assertNotEquals(firstUrl, secondUrl);
            assertEquals(200, get(browser, base + secondUrl, userId).statusCode());
            assertEquals(404, get(browser, base + firstUrl, userId).statusCode());
            assertEquals(secondUrl, app.getBean(ProfilePhotoService.class).current(userId));
            // Leave no fixture data in the disposable DB.
            app.getBean(JdbcClient.class).sql("DELETE FROM users WHERE id = :id").param("id", userId).update();
        }
    }

    private ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(LetsDoSportsApplication.class).run(
                "--server.port=0",
                "--spring.datasource.url=" + System.getenv("PROFILE_TEST_JDBC_URL"),
                "--spring.datasource.username=" + System.getenv("PROFILE_TEST_DB_USER"),
                "--spring.datasource.password=" + System.getenv("PROFILE_TEST_DB_PASSWORD"),
                "--app.profile-photos.directory=" + directory,
                "--spring.security.user.name=photo-test", "--spring.security.user.password=photo-test-only");
    }

    private static String base(ConfigurableApplicationContext app) {
        return "http://localhost:" + app.getEnvironment().getProperty("local.server.port");
    }

    private static HttpClient browser() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }

    private static HttpRequest.Builder request(String url, long userId) {
        return HttpRequest.newBuilder(URI.create(url)).header("Accept", "application/json")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("photo-test:photo-test-only".getBytes(StandardCharsets.UTF_8)))
                .header("X-User-Id", Long.toString(userId));
    }

    private static HttpResponse<String> get(HttpClient client, String url, long userId) throws Exception {
        return client.send(request(url, userId).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> upload(HttpClient client, String base, long userId, String csrf, byte[] png) throws Exception {
        String boundary = "photo-test-boundary";
        var body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"test.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(png);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return client.send(request(base + "/api/v1/users/me/photo", userId)
                .header(field(csrf, "csrfHeaderName"), field(csrf, "csrfToken"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String field(String json, String name) {
        var matcher = Pattern.compile("\"" + name + "\":\"([^\"]+)\"").matcher(json);
        assertTrue(matcher.find(), json);
        return matcher.group(1);
    }
}
