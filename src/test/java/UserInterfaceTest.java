import org.example.digital_scroll_management.DigitalScroll;
import org.example.digital_scroll_management.DigitalScrollRepository;
import org.example.digital_scroll_management.DigitalScrollService;
import org.example.scroll_seeker.ScrollSeekerConsole;
import org.example.scroll_seeker.ScrollSeekerService;
import org.example.user_management.PasswordHasher;
import org.example.user_management.UserManagementUI;
import org.example.user_management.UserManager;
import org.example.user_management.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class UserInterfaceTest {

    @TempDir
    Path tempDir;

    private TestEnvironment createEnvironment(String input) throws Exception {
        Path userStore = tempDir.resolve("users.db");
        UserRepository userRepository = new UserRepository(userStore);
        UserManager userManager = new UserManager(userRepository, new PasswordHasher());

        Path scrollStore = tempDir.resolve("scrolls.db");
        Path uploads = tempDir.resolve("uploads");
        DigitalScrollRepository scrollRepository = new DigitalScrollRepository(scrollStore);
        DigitalScrollService scrollService = new DigitalScrollService(scrollRepository, uploads);
        ScrollSeekerService scrollSeekerService = new ScrollSeekerService(scrollService);

        Scanner scanner = new Scanner(new StringReader(input));
        ScrollSeekerConsole console = new ScrollSeekerConsole(scanner, scrollSeekerService);
        UserManagementUI ui = new UserManagementUI(scanner, userManager, scrollService, console);
        return new TestEnvironment(userManager, userRepository, scrollService, scrollSeekerService, ui);
    }

    private record TestEnvironment(UserManager userManager,
                                   UserRepository userRepository,
                                   DigitalScrollService scrollService,
                                   ScrollSeekerService scrollSeekerService,
                                   UserManagementUI ui) {
    }

    @Test
    void generalUserFlowWithProfileUpdates() throws Exception {
        TestEnvironment env = createEnvironment(String.join("\n",
                "2",
                "user1",
                "password123",
                "user1@example.com",
                "123456",
                "User One",
                "ID-1",
                "",
                "2",
                "1",
                "User One Updated",
                "2",
                "user1new@example.com",
                "3",
                "654321",
                "4",
                "pass45678",
                "5",
                "ID-NEW",
                "6",
                "5",
                "4"
        ) + "\n");
    }

    @Test
    void adminFlowIncludesScrollManagementAndUserActions() throws Exception {
        Path userStore = tempDir.resolve("users.db");
        UserRepository userRepository = new UserRepository(userStore);
        UserManager userManager = new UserManager(userRepository, new PasswordHasher());

        Path scrollStore = tempDir.resolve("scrolls.db");
        Path uploads = tempDir.resolve("uploads");
        DigitalScrollRepository scrollRepository = new DigitalScrollRepository(scrollStore);
        DigitalScrollService scrollService = new DigitalScrollService(scrollRepository, uploads);
        ScrollSeekerService scrollSeekerService = new ScrollSeekerService(scrollService);

        Path source = tempDir.resolve("admin.bin");
        Files.write(source, new byte[]{1, 2, 3});
        DigitalScroll scroll = scrollService.addScroll("admin", "AdminScroll", source.toString());
        String scrollId = scroll.getScrollId();
        String dateStr = scroll.getUploadTimestamp().toLocalDate().toString();

        Path downloadTarget = tempDir.resolve("download.bin");

        String input = String.join("\n",
                "1",
                "admin",
                "admin123",
                "3",
                "1",
                "1",
                "2",
                scrollId,
                downloadTarget.toString(),
                "4",
                scrollId,
                "3",
                "admin",
                scrollId,
                "Admin",
                dateStr,
                "5",
                "6",
                "7",
                "newuser",
                "newpass",
                "new@example.com",
                "999",
                "New User",
                "NEW-ID",
                "1",
                "6",
                "8",
                "newuser",
                "9",
                "10",
                "5",
                "4"
        ) + "\n";

        Scanner scanner = new Scanner(new StringReader(input));
        ScrollSeekerConsole console = new ScrollSeekerConsole(scanner, scrollSeekerService);
        UserManagementUI ui = new UserManagementUI(scanner, userManager, scrollService, console);

        ui.start();

        assertNull(userRepository.findByUsername("newuser"));
        assertTrue(scrollService.getScroll(scrollId).getDownloadCount() >= 0);
    }

    @Test
    void guestFlowOnlyBrowsesScrolls() throws Exception {
        TestEnvironment env = createEnvironment(String.join("\n",
                "3",
                "1",
                "1",
                "2",
                "5",
                "2",
                "4"
        ) + "\n");

        env.ui().start();
        assertNull(env.userRepository().findByUsername("guest"));
    }
}

