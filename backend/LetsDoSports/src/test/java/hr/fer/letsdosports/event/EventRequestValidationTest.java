package hr.fer.letsdosports.event;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class EventRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() { factory.close(); }

    private static final OffsetDateTime FUTURE = OffsetDateTime.now().plusDays(7);

    private static CreateEventRequest validCreate() {
        return new CreateEventRequest("Petak nogomet", "opis", (short) 1, 10L, FUTURE,
                60, 10, (short) 18, null, null);
    }

    private static Set<String> invalidFields(Object o) {
        return validator.validate(o).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static String messageFor(Object o, String field) {
        return validator.validate(o).stream()
                .filter(v -> v.getPropertyPath().toString().equals(field))
                .map(ConstraintViolation::getMessage)
                .findFirst().orElse(null);
    }

    // ---------- CreateEventRequest ----------

    @Test
    void create_valid_hasNoViolations() {
        assertThat(validator.validate(validCreate())).isEmpty();
    }

    @Test
    void create_blankName_isRejected() {
        var req = new CreateEventRequest("   ", null, (short) 1, 10L, FUTURE, 60, 10, (short) 18, null, null);
        assertThat(invalidFields(req)).contains("name");
    }

    @Test
    void create_nullName_isRejected() {
        var req = new CreateEventRequest(null, null, (short) 1, 10L, FUTURE, 60, 10, (short) 18, null, null);
        assertThat(invalidFields(req)).contains("name");
    }

    @Test
    void create_nameIsTrimmedByConstructor() {
        var req = new CreateEventRequest("  Košarka  ", null, (short) 1, 10L, FUTURE, 60, 10, (short) 18, null, null);
        assertThat(req.name()).isEqualTo("Košarka");
    }

    @Test
    void create_nameOf40Chars_isAccepted_41Rejected() {
        var ok = new CreateEventRequest("x".repeat(40), null, (short) 1, 10L, FUTURE, 60, 10, (short) 18, null, null);
        var bad = new CreateEventRequest("x".repeat(41), null, (short) 1, 10L, FUTURE, 60, 10, (short) 18, null, null);
        assertThat(invalidFields(ok)).doesNotContain("name");
        assertThat(invalidFields(bad)).contains("name");
    }

    @Test
    void create_missingRequiredFields_areAllReported() {
        var req = new CreateEventRequest("Naziv", null, null, null, null, null, null, null, null, null);
        assertThat(invalidFields(req))
                .containsExactlyInAnyOrder("sportId", "venueId", "scheduledAt", "maxParticipants", "minAge");
    }

    @Test
    void create_pastScheduledAt_isRejected() {
        var req = new CreateEventRequest("Naziv", null, (short) 1, 10L,
                OffsetDateTime.now().minusDays(1), 60, 10, (short) 18, null, null);
        assertThat(messageFor(req, "scheduledAt")).isEqualTo("Termin mora biti sada ili u budućnosti.");
    }

    @Test
    void create_maxParticipantsZero_isRejected() {
        var req = new CreateEventRequest("Naziv", null, (short) 1, 10L, FUTURE, 60, 0, (short) 18, null, null);
        assertThat(invalidFields(req)).contains("maxParticipants");
    }

    @Test
    void create_nonPositiveDuration_isRejected() {
        var req = new CreateEventRequest("Naziv", null, (short) 1, 10L, FUTURE, 0, 10, (short) 18, null, null);
        assertThat(invalidFields(req)).contains("expectedDurationMin");
    }

    @Test
    void create_minAgeBelow13_isRejected_13Accepted() {
        var bad = new CreateEventRequest("Naziv", null, (short) 1, 10L, FUTURE, 60, 10, (short) 12, null, null);
        var ok = new CreateEventRequest("Naziv", null, (short) 1, 10L, FUTURE, 60, 10, (short) 13, null, null);
        assertThat(messageFor(bad, "minAge"))
                .isEqualTo("Dob ne smije biti ispod minimalne dobi aplikacije (13).");
        assertThat(invalidFields(ok)).doesNotContain("minAge");
    }

    // ---------- UpdateEventRequest (partial update: everything optional) ----------

    @Test
    void update_allNull_isValid() {
        var req = new UpdateEventRequest(null, null, null, null, null, null, null, null, null, null);
        assertThat(validator.validate(req)).isEmpty();
    }

    @Test
    void update_blankName_isRejected() {
        var req = new UpdateEventRequest("   ", null, null, null, null, null, null, null, null, null);
        assertThat(messageFor(req, "name")).isEqualTo("Naziv ne smije biti prazan.");
    }

    @Test
    void update_nameTooLong_isRejected() {
        var req = new UpdateEventRequest("x".repeat(41), null, null, null, null, null, null, null, null, null);
        assertThat(invalidFields(req)).contains("name");
    }

    @Test
    void update_pastScheduledAt_isRejected() {
        var req = new UpdateEventRequest(null, null, null, null, OffsetDateTime.now().minusDays(1),
                null, null, null, null, null);
        assertThat(invalidFields(req)).contains("scheduledAt");
    }

    @Test
    void update_minAgeBelow13_isRejected() {
        var req = new UpdateEventRequest(null, null, null, null, null, null, null, (short) 12, null, null);
        assertThat(messageFor(req, "minAge")).isEqualTo("Dob ne smije biti ispod 13.");
    }

    @Test
    void update_maxParticipantsZero_isRejected() {
        var req = new UpdateEventRequest(null, null, null, null, null, null, 0, null, null, null);
        assertThat(invalidFields(req)).contains("maxParticipants");
    }
}
