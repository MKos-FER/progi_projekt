package hr.fer.letsdosports.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class PhotoStorageTest {
    @TempDir Path directory;

    static MockMultipartFile photo(String format) throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), format, output);
        return new MockMultipartFile("file", "../../untrusted." + format,
                format.equals("png") ? "image/png" : "image/jpeg", output.toByteArray());
    }

    @Test void pngAndJpegSurviveNewStorageInstance() throws Exception {
        var storage = new PhotoStorage(directory.toString());
        for (String format : new String[]{"png", "jpg"}) {
            String name = storage.save(photo(format));
            byte[] bytes = new PhotoStorage(directory.toString()).read(name);
            assertEquals(8, ImageIO.read(new java.io.ByteArrayInputStream(bytes)).getWidth());
            assertFalse(name.contains("untrusted"));
            storage.delete(name);
        }
        try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
    }

    @Test void rejectsFakeImageEmptyWrongMimeAndOversizedFiles() throws Exception {
        var storage = new PhotoStorage(directory.toString());
        assertStatus(400, () -> storage.save(new MockMultipartFile("file", "x.png", "image/png", "not an image".getBytes())));
        assertStatus(400, () -> storage.save(new MockMultipartFile("file", new byte[0])));
        assertStatus(400, () -> storage.save(new MockMultipartFile("file", "x.png", "image/jpeg", photo("png").getBytes())));
        assertStatus(413, () -> storage.save(new MockMultipartFile("file", "x.png", "image/png", new byte[(int) PhotoStorage.MAX_BYTES + 1])));
    }

    @Test void rejectsExcessiveDimensionsAndTraversal() throws Exception {
        var storage = new PhotoStorage(directory.toString());
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4097, 1, BufferedImage.TYPE_INT_RGB), "png", output);
        assertStatus(400, () -> storage.save(new MockMultipartFile("file", "x.png", "image/png", output.toByteArray())));
        assertStatus(404, () -> storage.read("../secret"));
        assertStatus(404, () -> storage.read("00000000-0000-0000-0000-000000000000.png"));
    }

    private static void assertStatus(int expected, org.junit.jupiter.api.function.Executable operation) {
        assertEquals(expected, assertThrows(ResponseStatusException.class, operation).getStatusCode().value());
    }
}
