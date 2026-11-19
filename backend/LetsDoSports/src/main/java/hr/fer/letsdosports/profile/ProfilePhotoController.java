package hr.fer.letsdosports.profile;

import hr.fer.letsdosports.auth.CurrentUserId;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ProfilePhotoController {
    private final ProfilePhotoService service;
    private final PhotoStorage storage;

    public ProfilePhotoController(ProfilePhotoService service, PhotoStorage storage) {
        this.service = service;
        this.storage = storage;
    }

    @GetMapping("/users/me/photo")
    public ResponseEntity<PhotoResponse> current(@CurrentUserId Long userId, HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response(service.current(userId), request));
    }

    @PutMapping(value = "/users/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PhotoResponse replace(@CurrentUserId Long userId, @RequestPart("file") MultipartFile file, HttpServletRequest request) {
        return response(service.replace(userId, file), request);
    }

    @GetMapping("/profile-photos/{name}")
    public ResponseEntity<byte[]> image(@PathVariable String name) {
        byte[] content = storage.read(name);
        return ResponseEntity.ok()
                .contentType(name.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(content);
    }

    private PhotoResponse response(String url, HttpServletRequest request) {
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return new PhotoResponse(url, csrf == null ? null : csrf.getToken(), csrf == null ? null : csrf.getHeaderName());
    }

    public record PhotoResponse(String profilePhotoUrl, String csrfToken, String csrfHeaderName) {}
}
