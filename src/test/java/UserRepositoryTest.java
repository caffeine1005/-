import org.example.user_management.User;
import org.example.user_management.UserRepository;
import org.example.user_management.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void loadLegacyAndInvalidEntries() throws Exception {
        Path storage = tempDir.resolve("users.db");
        String content = String.join(System.lineSeparator(),
                "",
                "legacy|hash|legacy@example.com|123|LEGACY-ID|ADMIN",
                "skipped|too|short",
                "modern|hash2|modern@example.com|456|Modern Name|MOD-1|GENERAL",
                "mystery|hash3|mystery@example.com|789|Mystery Name|MYS-1|WIZARD"
        );
        Files.writeString(storage, content);

        UserRepository repository = new UserRepository(storage);

        User legacy = repository.findByUsername("legacy");
        assertNotNull(legacy);
        assertEquals("legacy", legacy.getFullName()); // legacy format defaults full name
        assertEquals(UserType.ADMIN, legacy.getUserType());
        assertEquals("LEGACY-ID", legacy.getCustomId());

        User modern = repository.findByUsername("modern");
        assertNotNull(modern);
        assertEquals("Modern Name", modern.getFullName());
        assertEquals(UserType.GENERAL, modern.getUserType());

        User mystery = repository.findByUsername("mystery");
        assertNotNull(mystery);
        assertEquals(UserType.GENERAL, mystery.getUserType()); // invalid type defaults to GENERAL
        assertEquals("mystery", repository.findByCustomId("mys-1").getUsername());

        assertTrue(repository.hasAdmin());
    }

    @Test
    void saveAndDeletePersistData() throws Exception {
        Path storage = tempDir.resolve("users.db");
        UserRepository repository = new UserRepository(storage);
        User user = new User("alpha", "hash", null, null, null, "A-1", UserType.GENERAL);
        repository.save(user);

        UserRepository reloaded = new UserRepository(storage);
        User persisted = reloaded.findByUsername("alpha");
        assertNotNull(persisted);
        assertEquals("", persisted.getEmail()); // null values persisted as empty strings
        assertEquals("", persisted.getFullName());

        repository.delete("alpha");
        assertTrue(repository.getAllUsers().isEmpty());
        assertNull(new UserRepository(storage).findByUsername("alpha"));
        assertNull(repository.findByCustomId(null));
        assertFalse(repository.hasAdmin());
    }
}
