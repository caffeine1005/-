import org.example.digital_scroll_management.DigitalScrollRepository;
import org.example.digital_scroll_management.DigitalScrollService;
import org.example.scroll_seeker.ScrollSeekerConsole;
import org.example.scroll_seeker.ScrollSeekerService;
import org.example.user_management.PasswordHasher;
import org.example.user_management.User;
import org.example.user_management.UserManagementUI;
import org.example.user_management.UserManager;
import org.example.user_management.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class UserManagementAdminMenuTest {

    @TempDir
    Path tempDir;

    @Test
    void adminMenuManagesUsers() throws Exception {
        Path userStore = tempDir.resolve("users.db");
        UserRepository userRepository = new UserRepository(userStore);
        UserManager userManager = new UserManager(userRepository, new PasswordHasher());
        userManager.ensureDefaultAdmin();
        User admin = userRepository.findByUsername("admin");

        Path scrollStore = tempDir.resolve("scrolls.db");
        Path uploads = tempDir.resolve("uploads");
        DigitalScrollRepository scrollRepository = new DigitalScrollRepository(scrollStore);
        DigitalScrollService scrollService = new DigitalScrollService(scrollRepository, uploads);
        ScrollSeekerService seekerService = new ScrollSeekerService(scrollService);

        String input = String.join("\n",
                "6",
                "7",
                "alpha",
                "alphaPass",
                "alpha@example.com",
                "777",
                "Alpha User",
                "ALPHA-ID",
                "1",
                "6",
                "8",
                "alpha",
                "9",
                "10"
        ) + "\n";

        Scanner scanner = new Scanner(new StringReader(input));
        ScrollSeekerConsole console = new ScrollSeekerConsole(scanner, seekerService);
        UserManagementUI ui = new UserManagementUI(scanner, userManager, scrollService, console);

        Field currentUser = UserManagementUI.class.getDeclaredField("currentUser");
        currentUser.setAccessible(true);
        currentUser.set(ui, admin);

        Method adminMenu = UserManagementUI.class.getDeclaredMethod("adminScrollMenu");
        adminMenu.setAccessible(true);
        adminMenu.invoke(ui);

        assertNull(userRepository.findByUsername("alpha"));
    }
}