package hr.fer.letsdosports.profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProfilePhotoServiceTest {
    private final ProfilePhotoRepository repository = mock(ProfilePhotoRepository.class);
    private final PhotoStorage storage = mock(PhotoStorage.class);
    private final TransactionTemplate transactions = mock(TransactionTemplate.class);
    private final ProfilePhotoService service = new ProfilePhotoService(repository, storage, transactions);

    @BeforeEach void transactionsRunCallback() {
        when(transactions.execute(any())).thenAnswer(inv ->
                ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(mock(TransactionStatus.class)));
    }

    @Test void profileWithoutPhotoIsValid() {
        when(repository.findUrl(1L, false)).thenReturn(null);
        assertNull(service.current(1L));
        verifyNoInteractions(storage);
    }

    @Test void replaceCommitsBeforeDeletingPreviousPhoto() throws Exception {
        var file = PhotoStorageTest.photo("png");
        when(storage.save(file)).thenReturn("new.png");
        when(repository.findUrl(1L, true)).thenReturn(ProfilePhotoService.PREFIX + "old.png");
        assertEquals(ProfilePhotoService.PREFIX + "new.png", service.replace(1L, file));
        var order = inOrder(repository, transactions, storage);
        order.verify(storage).save(file);
        order.verify(transactions).execute(any());
        order.verify(repository).findUrl(1L, true);
        order.verify(repository).update(1L, ProfilePhotoService.PREFIX + "new.png");
        order.verify(storage).delete("old.png");
    }

    @Test void failedCommitRemovesNewFileAndKeepsOldPhoto() throws Exception {
        var file = PhotoStorageTest.photo("png");
        when(storage.save(file)).thenReturn("new.png");
        doThrow(new IllegalStateException("commit failed")).when(transactions).execute(any());
        assertThrows(IllegalStateException.class, () -> service.replace(1L, file));
        verify(storage).delete("new.png");
        verify(storage, never()).delete("old.png");
    }

    @Test void cleanupFailureDoesNotHideSuccessfulReplacement() throws Exception {
        var file = PhotoStorageTest.photo("png");
        when(storage.save(file)).thenReturn("new.png");
        when(repository.findUrl(1L, true)).thenReturn(ProfilePhotoService.PREFIX + "old.png");
        doThrow(new IOException("read only")).when(storage).delete("old.png");
        assertEquals(ProfilePhotoService.PREFIX + "new.png", service.replace(1L, file));
    }

    @Test void externalPhotoUrlIsNeverDeletedAsLocalFile() throws Exception {
        var file = PhotoStorageTest.photo("png");
        when(storage.save(file)).thenReturn("new.png");
        when(repository.findUrl(1L, true)).thenReturn("https://example.com/avatar.png");
        service.replace(1L, file);
        verify(storage, never()).delete(any());
    }
}
