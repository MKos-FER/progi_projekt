package hr.fer.letsdosports.profile;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class ProfilePhotoRepository {
    private final JdbcClient jdbc;

    public ProfilePhotoRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public String findUrl(long userId, boolean lock) {
        return jdbc.sql("SELECT profile_photo_url FROM users WHERE id = :id" + (lock ? " FOR UPDATE" : ""))
                .param("id", userId)
                .query((rs, row) -> new PhotoRow(rs.getString("profile_photo_url")))
                .optional().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Korisnik ne postoji.")).url();
    }

    public void update(long userId, String url) {
        if (jdbc.sql("UPDATE users SET profile_photo_url = :url WHERE id = :id")
                .param("url", url).param("id", userId).update() != 1)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Korisnik ne postoji.");
    }

    private record PhotoRow(String url) {}
}
