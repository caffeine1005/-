import org.example.digital_scroll_management.DigitalScroll;
import org.example.digital_scroll_management.DigitalScrollRepository;
import org.example.digital_scroll_management.DigitalScrollService;
import org.example.scroll_seeker.ScrollSeekerConsole;
import org.example.scroll_seeker.ScrollSeekerService;
import org.example.user_management.PasswordHasher;
import org.example.user_management.User;
import org.example.user_management.UserManagementUI;
import org.example.user_management.UserManager;
import org.example.user_management.UserRepository;
import org.example.user_management.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class UserManagementUISupportTest {

    @TempDir
    Path tempDir;

    private record UiFixture(UserManagementUI ui,
                             UserManager manager,
                             UserRepository repository,
                             DigitalScrollService scrollService) {
    }

    private UiFixture createFixture(String input) throws Exception {
        return createFixture(input, null);
    }

    private UiFixture createFixture(String input, DigitalScrollService existingScrollService) throws Exception {
        Scanner scanner = new Scanner(new StringReader(input));
        Path userStore = Files.createTempFile(tempDir, "users", ".db");
        UserRepository userRepository = new UserRepository(userStore);
        UserManager userManager = new UserManager(userRepository, new PasswordHasher());

        DigitalScrollService scrollService;
        if (existingScrollService != null) {
            scrollService = existingScrollService;
        } else {
            Path scrollStore = Files.createTempFile(tempDir, "scrolls", ".db");
            Path uploads = tempDir.resolve("uploads-" + System.nanoTime());
            DigitalScrollRepository scrollRepository = new DigitalScrollRepository(scrollStore);
            scrollService = new DigitalScrollService(scrollRepository, uploads);
        }
        ScrollSeekerService scrollSeekerService = new ScrollSeekerService(scrollService);
        ScrollSeekerConsole console = new ScrollSeekerConsole(scanner, scrollSeekerService);

        UserManagementUI ui = new UserManagementUI(scanner, userManager, scrollService, console);
        return new UiFixture(ui, userManager, userRepository, scrollService);
    }

    private Method method(Class<?> type, String name, Class<?>... params) throws NoSuchMethodException {
        Method method = type.getDeclaredMethod(name, params);
        method.setAccessible(true);
        return method;
    }

    private Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @Test
    void startHandlesClosedInputGracefully() throws Exception {
        UiFixture fixture = createFixture("");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out));
        try {
            fixture.ui().start();
        } finally {
            System.setOut(original);
        }
        assertTrue(out.toString().contains("Input stream closed. Exiting application."));
    }

    @Test
    void chooseFileHeadlessCancellationReturnsNull() throws Exception {
        UiFixture fixture = createFixture(System.lineSeparator());
        Method chooseFile = method(UserManagementUI.class, "chooseFile");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out));
        Object result;
        try {
            result = chooseFile.invoke(fixture.ui());
        } finally {
            System.setOut(original);
        }
        assertNull(result);
        String output = out.toString();
        assertTrue(output.contains("GUI file chooser is not available"));
        assertTrue(output.contains("File selection cancelled."));
    }

    @Test
    void chooseFileHeadlessReturnsManualPath() throws Exception {
        String path = tempDir.resolve("manual.bin").toString();
        UiFixture fixture = createFixture(path + System.lineSeparator());
        Method chooseFile = method(UserManagementUI.class, "chooseFile");

        String result = (String) chooseFile.invoke(fixture.ui());
        assertEquals(path, result);
    }

    @Test
    void readUserTypeRetriesUntilValid() throws Exception {
        UiFixture fixture = createFixture(String.join(System.lineSeparator(), "0", "3", "2") + System.lineSeparator());
        Method readUserType = method(UserManagementUI.class, "readUserType");

        UserType type = (UserType) readUserType.invoke(fixture.ui());
        assertEquals(UserType.ADMIN, type);
    }

    @Test
    void updateFullNameHandlesValidationError() throws Exception {
        UiFixture fixture = createFixture("   " + System.lineSeparator());
        User user = fixture.manager().registerGeneralUser("wizard", "spell123", "wiz@example.com",
                "000", "Wizard", "WZ-1");
        Field currentUser = field(UserManagementUI.class, "currentUser");
        currentUser.set(fixture.ui(), user);
        Method updateFullName = method(UserManagementUI.class, "updateFullName");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out));
        try {
            updateFullName.invoke(fixture.ui());
        } finally {
            System.setOut(original);
        }

        assertEquals("Wizard", user.getFullName());
        assertTrue(out.toString().contains("Update failed"));
    }

    @Test
    void getDisplayNameFallsBackToUsername() throws Exception {
        UiFixture fixture = createFixture("");
        User user = new User("rogue", "hash", "r@example.com", "111", null, "RG-1", UserType.GENERAL);
        Field currentUser = field(UserManagementUI.class, "currentUser");
        currentUser.set(fixture.ui(), user);

        Method getDisplayName = method(UserManagementUI.class, "getDisplayName");
        String display = (String) getDisplayName.invoke(fixture.ui());
        assertTrue(display.startsWith("rogue (general)"));
    }

    @Test
    void addScrollRequiresCurrentUser() throws Exception {
        UiFixture fixture = createFixture("");
        Method addScroll = method(UserManagementUI.class, "addScroll");
        addScroll.invoke(fixture.ui());
        assertTrue(fixture.scrollService().listAllScrolls().isEmpty());
    }

    @Test
    void addScrollCancelsWhenNoFileSelected() throws Exception {
        String input = "Mystic Scroll" + System.lineSeparator() + System.lineSeparator();
        UiFixture fixture = createFixture(input);
        User user = fixture.manager().registerGeneralUser("mage", "spell", "m@example.com", "000", "Mage", "MG-1");
        Field currentUser = field(UserManagementUI.class, "currentUser");
        currentUser.set(fixture.ui(), user);

        Method addScroll = method(UserManagementUI.class, "addScroll");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out));
        try {
            addScroll.invoke(fixture.ui());
        } finally {
            System.setOut(original);
        }

        assertTrue(fixture.scrollService().listAllScrolls().isEmpty());
        assertTrue(out.toString().contains("File selection cancelled."));
    }

    @Test
    void addScrollUploadsFileSuccessfully() throws Exception {
        Path source = tempDir.resolve("upload.bin");
        Files.write(source, new byte[]{1, 2, 3});
        String input = "Arcane Scroll" + System.lineSeparator() + source.toString() + System.lineSeparator();
        UiFixture fixture = createFixture(input);
        User user = fixture.manager().registerGeneralUser("mage", "spell", "m@example.com", "000", "Mage", "MG-2");
        Field currentUser = field(UserManagementUI.class, "currentUser");
        currentUser.set(fixture.ui(), user);

        Method addScroll = method(UserManagementUI.class, "addScroll");
        addScroll.invoke(fixture.ui());

        assertEquals(1, fixture.scrollService().listScrollsByOwner("mage").size());
    }

    @Test
    void updateOperationsAreNoOpsWithoutUser() throws Exception {
        UiFixture fixture = createFixture("");
        Field currentUser = field(UserManagementUI.class, "currentUser");
        currentUser.set(fixture.ui(), null);

        method(UserManagementUI.class, "updateFullName").invoke(fixture.ui());
        method(UserManagementUI.class, "updateEmail").invoke(fixture.ui());
        method(UserManagementUI.class, "updatePhone").invoke(fixture.ui());
        method(UserManagementUI.class, "updatePassword").invoke(fixture.ui());
        method(UserManagementUI.class, "updateCustomId").invoke(fixture.ui());
        method(UserManagementUI.class, "showProfile").invoke(fixture.ui());
        method(UserManagementUI.class, "listMyScrolls").invoke(fixture.ui());
        method(UserManagementUI.class, "removeScroll").invoke(fixture.ui());
    }

    @Test
    void handleLoginFailsWithInvalidCredentials() throws Exception {
        UiFixture fixture = createFixture("nope" + System.lineSeparator() + "wrong" + System.lineSeparator());
        Method handleLogin = method(UserManagementUI.class, "handleLogin");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out));
        try {
            handleLogin.invoke(fixture.ui());
        } finally {
            System.setOut(original);
        }

        assertTrue(out.toString().contains("Login failed"));
        Field currentUser = field(UserManagementUI.class, "currentUser");
        assertNull(currentUser.get(fixture.ui()));
    }

    @Test
    void editScrollUpdatesNameWithoutReplacingFile() throws Exception {
        Path scrollStore = Files.createTempFile(tempDir, "edit-scrolls", ".db");
        Path uploads = tempDir.resolve("uploads-edit-" + System.nanoTime());
        DigitalScrollRepository repository = new DigitalScrollRepository(scrollStore);
        DigitalScrollService scrollService = new DigitalScrollService(repository, uploads);
        Path source = tempDir.resolve("original.bin");
        Files.write(source, new byte[]{4, 5});
        DigitalScroll scroll = scrollService.addScroll("mage", "Original", source.toString());

        String input = String.join(System.lineSeparator(),
                scroll.getScrollId(),
                "Renamed",
                "n") + System.lineSeparator();
        UiFixture fixture = createFixture(input, scrollService);
        User user = fixture.manager().registerGeneralUser("mage", "spell", "m@example.com", "000", "Mage", "MG-3");
        Field currentUser = field(UserManagementUI.class, "currentUser");
        currentUser.set(fixture.ui(), user);

        Method editScroll = method(UserManagementUI.class, "editScroll");
        editScroll.invoke(fixture.ui());

        assertEquals("Renamed", fixture.scrollService().getScroll(scroll.getScrollId()).getName());
    }

    @Test
    void editScrollCancelsWhenReplacementMissing() throws Exception {
        Path scrollStore = Files.createTempFile(tempDir, "edit-cancel-scrolls", ".db");
        Path uploads = tempDir.resolve("uploads-edit-cancel-" + System.nanoTime());
        DigitalScrollRepository repository = new DigitalScrollRepository(scrollStore);
        DigitalScrollService scrollService = new DigitalScrollService(repository, uploads);
        Path source = tempDir.resolve("original-cancel.bin");
        Files.write(source, new byte[]{7});
        DigitalScroll scroll = scrollService.addScroll("mage", "Original", source.toString());

        String input = String.join(System.lineSeparator(),
                scroll.getScrollId(),
                "",
                "y",
                "") + System.lineSeparator();
        UiFixture fixture = createFixture(input, scrollService);
        User user = fixture.manager().registerGeneralUser("mage", "spell", "m@example.com", "000", "Mage", "MG-4");
        Field currentUser = field(UserManagementUI.class, "currentUser");
        currentUser.set(fixture.ui(), user);

        Method editScroll = method(UserManagementUI.class, "editScroll");
        editScroll.invoke(fixture.ui());

        assertEquals("Original", fixture.scrollService().getScroll(scroll.getScrollId()).getName());
    }

    @Test
    void removeScrollDeletesOwnedScroll() throws Exception {
        Path scrollStore = Files.createTempFile(tempDir, "remove-scrolls", ".db");
        Path uploads = tempDir.resolve("uploads-remove-" + System.nanoTime());
        DigitalScrollRepository repository = new DigitalScrollRepository(scrollStore);
        DigitalScrollService scrollService = new DigitalScrollService(repository, uploads);
        Path source = tempDir.resolve("remove.bin");
        Files.write(source, new byte[]{1});
        DigitalScroll scroll = scrollService.addScroll("mage", "ToRemove", source.toString());

        String input = scroll.getScrollId() + System.lineSeparator();
        UiFixture fixture = createFixture(input, scrollService);
        User user = fixture.manager().registerGeneralUser("mage", "spell", "m@example.com", "000", "Mage", "MG-5");
        Field currentUser = field(UserManagementUI.class, "currentUser");
        currentUser.set(fixture.ui(), user);

        Method removeScroll = method(UserManagementUI.class, "removeScroll");
        removeScroll.invoke(fixture.ui());

        assertNull(fixture.scrollService().getScroll(scroll.getScrollId()));
    }

    @Test
    void accountMenuHandlesInvalidOption() throws Exception {
        String input = String.join(System.lineSeparator(), "x", "5") + System.lineSeparator();
        UiFixture fixture = createFixture(input);
        User user = fixture.manager().registerGeneralUser("mage", "spell", "m@example.com", "000", "Mage", "MG-6");
        Field currentUser = field(UserManagementUI.class, "currentUser");
        currentUser.set(fixture.ui(), user);

        Method accountMenu = method(UserManagementUI.class, "accountMenu");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out));
        try {
            accountMenu.invoke(fixture.ui());
        } finally {
            System.setOut(original);
        }
        assertTrue(out.toString().contains("Invalid option, please try again."));
    }

    @Test
    void userScrollMenuShowsMessageForInvalidChoice() throws Exception {
        String input = String.join(System.lineSeparator(), "9", "5") + System.lineSeparator();
        UiFixture fixture = createFixture(input);
        User user = fixture.manager().registerGeneralUser("mage", "spell", "m@example.com", "000", "Mage", "MG-7");
        Field currentUser = field(UserManagementUI.class, "currentUser");
        currentUser.set(fixture.ui(), user);

        Method userScrollMenu = method(UserManagementUI.class, "userScrollMenu");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out));
        try {
            userScrollMenu.invoke(fixture.ui());
        } finally {
            System.setOut(original);
        }
        assertTrue(out.toString().contains("Invalid option, please try again."));
    }

    @Test
    void guestMenuHandlesInvalidChoice() throws Exception {
        String input = String.join(System.lineSeparator(), "3", "2") + System.lineSeparator();
        UiFixture fixture = createFixture(input);
        User guest = new User("guest", "", "", "", "", "guest-id", UserType.GUEST);
        Field currentUser = field(UserManagementUI.class, "currentUser");
        currentUser.set(fixture.ui(), guest);

        Method guestMenu = method(UserManagementUI.class, "guestMenu");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out));
        try {
            guestMenu.invoke(fixture.ui());
        } finally {
            System.setOut(original);
        }
        assertTrue(out.toString().contains("Invalid option, please try again."));
    }
}
