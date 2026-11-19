package hr.fer.letsdosports.profile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class PhotoStorage {
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;
    private static final Pattern NAME = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg)");
    private final Path root;

    public PhotoStorage(@Value("${app.profile-photos.directory:${user.home}/.lets-do-sports/profile-photos}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }

    public String save(MultipartFile file) {
        if (file.isEmpty()) throw invalid("Odaberite fotografiju.");
        if (file.getSize() > MAX_BYTES) throw tooLarge();
        try {
            byte[] bytes;
            try (InputStream stream = file.getInputStream()) {
                bytes = stream.readNBytes((int) MAX_BYTES + 1);
            }
            if (bytes.length > MAX_BYTES) throw tooLarge();
            BufferedImage image;
            String format;
            // Read dimensions before decoding to bound decompressed memory usage.
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw invalid("Datoteka nije valjana JPEG ili PNG fotografija.");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input);
                    format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    String mime = switch (format) {
                        case "jpeg", "jpg" -> "image/jpeg";
                        case "png" -> "image/png";
                        default -> throw invalid("Dopuštene su samo JPEG i PNG fotografije.");
                    };
                    if (!mime.equalsIgnoreCase(file.getContentType())) throw invalid("Vrsta datoteke ne odgovara sadržaju fotografije.");
                    if (reader.getWidth(0) > MAX_DIMENSION || reader.getHeight(0) > MAX_DIMENSION)
                        throw invalid("Fotografija smije imati najviše 4096 × 4096 piksela.");
                    image = reader.read(0);
                } finally {
                    reader.dispose();
                }
            } catch (IOException | IllegalArgumentException ex) {
                throw invalid("Fotografiju nije moguće pročitati.");
            }
            // Re-encode pixels: original filenames, metadata and trailing payloads are not stored.
            String extension = format.equals("png") ? "png" : "jpg";
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(image, extension, output)) throw invalid("Fotografiju nije moguće obraditi.");
            if (output.size() > MAX_BYTES) throw tooLarge();
            Files.createDirectories(root);
            String name = UUID.randomUUID() + "." + extension;
            Path target = path(name);
            try {
                Files.write(target, output.toByteArray(), StandardOpenOption.CREATE_NEW);
            } catch (IOException ex) {
                Files.deleteIfExists(target);
                throw ex;
            }
            return name;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Pohrana fotografije trenutačno nije dostupna.", ex);
        }
    }

    public byte[] read(String name) {
        Path target = path(name);
        try {
            if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) throw notFound();
            return Files.readAllBytes(target);
        } catch (NoSuchFileException ex) {
            throw notFound();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Fotografija trenutačno nije dostupna.", ex);
        }
    }

    public void delete(String name) throws IOException {
        Files.deleteIfExists(path(name));
    }

    private Path path(String name) {
        if (name == null || !NAME.matcher(name).matches()) throw notFound();
        return root.resolve(name);
    }

    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException tooLarge() {
        return new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Fotografija smije imati najviše 5 MiB.");
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Fotografija ne postoji.");
    }
}
