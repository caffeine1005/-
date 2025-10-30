import org.example.user_management.PasswordHasher;
import org.example.user_management.User;
import org.example.user_management.UserManager;
import org.example.user_management.UserRepository;
import org.example.user_management.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class UserManagerTest {

    @TempDir
    Path tempDir;

    private UserManager createManager() {
        Path storage = tempDir.resolve("users.db");
        UserRepository repository = new UserRepository(storage);
        PasswordHasher hasher = new PasswordHasher();
        return new UserManager(repository, hasher);
    }

    @Test
    void ensureDefaultAdminCreatesAdminOnce() {
        UserManager manager = createManager();
        manager.ensureDefaultAdmin();
        manager.ensureDefaultAdmin();

        UserRepository repo = new UserRepository(tempDir.resolve("users.db"));
        User admin = repo.findByUsername("admin");
        assertNotNull(admin);
        assertEquals(UserType.ADMIN, admin.getUserType());
    }

    @Test
    void registerAndUpdateUserProfile() {
        UserManager manager = createManager();
        manager.ensureDefaultAdmin();

        User user = manager.registerGeneralUser("zeyuan", "secret123", "z@example.com",
                "123456789", "Zeyuan Z", "ZEY-01");
        assertEquals("Zeyuan Z", user.getFullName());
        assertEquals("ZEY-01", user.getCustomId());

        assertNotNull(manager.login("zeyuan", "secret123"));
        assertNull(manager.login("zeyuan", "wrong"));

        manager.updateFullName(user, "New Name");
        manager.updateEmail(user, "new@example.com");
        manager.updatePhone(user, "987654321");
        manager.changePassword(user, "newPass!");
        manager.updateCustomId(user, "ZEY-02");

        assertEquals("New Name", user.getFullName());
        assertEquals("new@example.com", user.getEmail());
        assertEquals("987654321", user.getPhoneNumber());
        assertNotNull(manager.login("zeyuan", "newPass!"));
        assertEquals("ZEY-02", user.getCustomId());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> manager.updateCustomId(user, "admin"));
        assertTrue(ex.getMessage().contains("unique"));
    }

    @Test
    void createUserRejectsDuplicates() {
        UserManager manager = createManager();
        manager.registerGeneralUser("alpha", "pass123", "a@example.com", "111", "Alpha", "A-1");

        assertThrows(IllegalArgumentException.class,
                () -> manager.registerGeneralUser("alpha", "pass123", "b@example.com", "222", "Beta", "B-1"));
        assertThrows(IllegalArgumentException.class,
                () -> manager.registerGeneralUser("beta", "pass123", "b@example.com", "222", "Beta", "A-1"));
    }

    @Test
    void guestUserHasGuestTypeAndId() {
        UserManager manager = createManager();
        User guest = manager.createGuestUser();
        assertEquals(UserType.GUEST, guest.getUserType());
        assertTrue(guest.getCustomId().startsWith("guest-"));
    }

    @Test
    void deleteUserRemovesNonAdmin() throws Exception {
        UserManager manager = createManager();
        manager.ensureDefaultAdmin();
        manager.registerGeneralUser("normal", "pass123", "n@example.com", "000", "Normal User", "N-1");

        manager.deleteUser("normal");

        assertNull(manager.login("normal", "pass123"));

        String content = Files.readString(tempDir.resolve("users.db"));
        assertFalse(content.contains("normal"));
    }

    @Test
    void updateFullNameRejectsBlank() {
        UserManager manager = createManager();
        User user = manager.registerGeneralUser("u1", "password", "a@b.com", "111", "Alpha", "ID-1");
        assertThrows(IllegalArgumentException.class, () -> manager.updateFullName(user, "  "));
    }

    @Test
    void updateCustomIdRejectsExistingId() {
        UserManager manager = createManager();
        manager.registerGeneralUser("user1", "password", "u1@a.com", "111", "User One", "ID-1");
        User user2 = manager.registerGeneralUser("user2", "password", "u2@a.com", "222", "User Two", "ID-2");

        assertThrows(IllegalArgumentException.class, () -> manager.updateCustomId(user2, "ID-1"));
        assertEquals("ID-2", user2.getCustomId());
    }

    @Test
    void loginTrimsWhitespace() {
        UserManager manager = createManager();
        manager.registerGeneralUser("alice", "password", "a@b.com", "111", "Alice", "AL-1");
        assertNotNull(manager.login(" alice ", "password"));
    }

    @Test
    void registerGeneralUserTrimsInputs() {
        UserManager manager = createManager();
        User user = manager.registerGeneralUser(" bob ", "password", " b@c.com ", " 333 ", " Bob B ", " BB-1 ");
        assertEquals("bob", user.getUsername());
        assertEquals("b@c.com", user.getEmail());
        assertEquals("333", user.getPhoneNumber());
        assertEquals("Bob B", user.getFullName());
        assertEquals("BB-1", user.getCustomId());
    }

    @Test
    void changePasswordRejectsEmpty() {
        UserManager manager = createManager();
        User user = manager.registerGeneralUser("short", "initial1", "s@c.com", "444", "Shorty", "SH-1");
        assertThrows(IllegalArgumentException.class, () -> manager.changePassword(user, " "));
    }

    @Test
    void deleteUserRejectsAdmin() {
        UserManager manager = createManager();
        manager.ensureDefaultAdmin();
        assertThrows(IllegalArgumentException.class, () -> manager.deleteUser("admin"));
    }

    @Test
    void ensureDefaultAdminCustomIdStable() {
        UserManager manager = createManager();
        manager.ensureDefaultAdmin();
        User admin = new UserRepository(tempDir.resolve("users.db")).findByUsername("admin");
        assertNotNull(admin.getCustomId());
        assertFalse(admin.getCustomId().isEmpty());
    }

    @Test
    void registerGeneralUserRejectsEmptyFields() {
        UserManager manager = createManager();
        assertThrows(IllegalArgumentException.class,
                () -> manager.registerGeneralUser("", "password", "a@b.com", "111", "Name", "ID"));
        assertThrows(IllegalArgumentException.class,
                () -> manager.registerGeneralUser("user", "password", "a@b.com", "111", "", "ID"));
    }

    @Test
    void ensureDefaultAdminHandlesExistingCustomId() {
        Path storage = tempDir.resolve("users.db");
        UserRepository repository = new UserRepository(storage);
        repository.save(new User("existing", "hash", "e@example.com", "000", "Existing", "admin", UserType.GENERAL));
        UserManager manager = new UserManager(repository, new PasswordHasher());

        manager.ensureDefaultAdmin();

        User admin = repository.findByUsername("admin");
        assertNotNull(admin);
        assertNotEquals("admin", admin.getCustomId());
        assertTrue(admin.getCustomId().startsWith("admin"));
    }

    @Test
    void deleteUserValidatesInput() {
        UserManager manager = createManager();
        assertThrows(IllegalArgumentException.class, () -> manager.deleteUser(null));
        assertThrows(IllegalArgumentException.class, () -> manager.deleteUser("missing"));
    }
}
