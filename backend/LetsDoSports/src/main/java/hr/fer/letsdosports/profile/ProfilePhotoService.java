package hr.fer.letsdosports.profile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfilePhotoService {
    public static final String PREFIX = "/api/v1/profile-photos/";
    private static final Logger log = LoggerFactory.getLogger(ProfilePhotoService.class);
    private final ProfilePhotoRepository repository;
    private final PhotoStorage storage;
    private final TransactionTemplate transactions;

    public ProfilePhotoService(ProfilePhotoRepository repository, PhotoStorage storage, TransactionTemplate transactions) {
        this.repository = repository;
        this.storage = storage;
        this.transactions = transactions;
    }

    public String current(long userId) { return repository.findUrl(userId, false); }

    public String replace(long userId, MultipartFile file) {
        String name = storage.save(file);
        String url = PREFIX + name;
        String previous;
        try {
            previous = transactions.execute(status -> {
                // Serialize concurrent replacements for this user.
                String old = repository.findUrl(userId, true);
                repository.update(userId, url);
                return old;
            });
        } catch (RuntimeException ex) {
            removeManagedFile(url);
            throw ex;
        }
        // Only delete the old file after a successful database commit.
        removeManagedFile(previous);
        return url;
    }

    private void removeManagedFile(String url) {
        if (url == null || !url.startsWith(PREFIX)) return;
        try {
            storage.delete(url.substring(PREFIX.length()));
        } catch (Exception ex) {
            // A cleanup failure must not turn a committed replacement into an error response.
            log.warn("Could not remove obsolete profile photo", ex);
        }
    }
}
