package hr.fer.letsdosports.usersport;

import hr.fer.letsdosports.event.ApiValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Treba pokrenutu bazu s migracijama do V7. Namjerno bez @Transactional na klasi:
// provjera "barem jedan sport" u bazi je odgođena do COMMIT-a, pa testovi stvarno potvrđuju
// transakcije. Svaki test sam stvara korisnika i briše ga na kraju (kaskadno sa sportovima).
@SpringBootTest
class UserSportServiceIntegrationTest {

    @Autowired UserSportService service;
    @Autowired JdbcClient jdbc;
    @Autowired TransactionTemplate tx;

    private long userId;
    private short sport1;
    private short sport2;

    @BeforeEach
    void createUser() {
        List<Short> sports = jdbc.sql("SELECT id FROM sports ORDER BY id LIMIT 2").query(Short.class).list();
        sport1 = sports.get(0);
        sport2 = sports.get(1);
        userId = tx.execute(status -> {
            long id = jdbc.sql("""
                            INSERT INTO users (username, name, gender, date_of_birth, city_id)
                            VALUES (:username, 'Test Sportovi', 'Other', DATE '2000-01-01',
                                    (SELECT min(id) FROM sif_city))
                            RETURNING id""")
                    .param("username", "test-p02-" + UUID.randomUUID())
                    .query(Long.class).single();
            jdbc.sql("INSERT INTO user_sports (user_id, sport_id, skill_level_id) VALUES (:u, :s, 1)")
                    .param("u", id).param("s", sport1).update();
            return id;
        });
    }

    @AfterEach
    void deleteUser() {
        tx.executeWithoutResult(status ->
                jdbc.sql("DELETE FROM users WHERE id = :u").param("u", userId).update());
    }

    private List<String> storedSports() {
        return jdbc.sql("SELECT sport_id || ':' || skill_level_id FROM user_sports WHERE user_id = :u ORDER BY sport_id")
                .param("u", userId).query(String.class).list();
    }

    @Test
    void replace_replacesWholeListAndPersistsAfterCommit() {
        tx.executeWithoutResult(status -> service.replace(userId, List.of(
                new SportSelection(sport2, (short) 3),
                new SportSelection(sport1, (short) 2))));

        // Novo čitanje izvan transakcije: vrijedi ono što je potvrđeno
        assertThat(storedSports()).containsExactly(sport1 + ":2", sport2 + ":3");

        tx.executeWithoutResult(status -> service.replace(userId, List.of(new SportSelection(sport2, (short) 1))));
        assertThat(storedSports()).containsExactly(sport2 + ":1");
    }

    @Test
    void replace_emptyOrNull_rejected() {
        assertThatThrownBy(() -> service.replace(userId, List.of()))
                .isInstanceOfSatisfying(ApiValidationException.class, e ->
                        assertThat(e.getErrors()).containsEntry("sports", "Potreban je barem jedan sport."));
        assertThatThrownBy(() -> service.replace(userId, null))
                .isInstanceOf(ApiValidationException.class);
        assertThat(storedSports()).containsExactly(sport1 + ":1");
    }

    @Test
    void replace_duplicateSport_rejected() {
        assertThatThrownBy(() -> service.replace(userId, List.of(
                new SportSelection(sport2, (short) 1),
                new SportSelection(sport2, (short) 3))))
                .isInstanceOfSatisfying(ApiValidationException.class, e ->
                        assertThat(e.getErrors()).containsEntry("sports", "Svaki sport smije biti naveden najviše jednom."));
        assertThat(storedSports()).containsExactly(sport1 + ":1");
    }

    @Test
    void replace_unknownSportOrSkillLevel_rejected() {
        assertThatThrownBy(() -> service.replace(userId, List.of(new SportSelection((short) 32000, (short) 1))))
                .isInstanceOfSatisfying(ApiValidationException.class, e ->
                        assertThat(e.getErrors()).containsEntry("sports", "Sport 32000 ne postoji."));
        assertThatThrownBy(() -> service.replace(userId, List.of(new SportSelection(sport2, (short) 32000))))
                .isInstanceOfSatisfying(ApiValidationException.class, e ->
                        assertThat(e.getErrors()).containsEntry("sports", "Razina 32000 ne postoji."));
        assertThat(storedSports()).containsExactly(sport1 + ":1");
    }

    @Test
    void database_rejectsDeletingLastSportAtCommit() {
        assertThatThrownBy(() -> tx.executeWithoutResult(status ->
                jdbc.sql("DELETE FROM user_sports WHERE user_id = :u").param("u", userId).update()))
                .rootCause().hasMessageContaining("mora imati barem jedan sport");

        assertThat(storedSports()).containsExactly(sport1 + ":1");
    }

    @Test
    void database_allowsDeletingUserWithSports() {
        tx.executeWithoutResult(status ->
                jdbc.sql("DELETE FROM users WHERE id = :u").param("u", userId).update());

        assertThat(jdbc.sql("SELECT count(*) FROM user_sports WHERE user_id = :u")
                .param("u", userId).query(Integer.class).single()).isZero();
    }
}
