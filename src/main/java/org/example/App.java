package org.example;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import org.example.digital_scroll_management.DigitalScrollRepository;
import org.example.digital_scroll_management.DigitalScrollService;
import org.example.scroll_seeker.ScrollSeekerConsole;
import org.example.scroll_seeker.ScrollSeekerService;
import org.example.user_management.PasswordHasher;
import org.example.user_management.UserManagementUI;
import org.example.user_management.UserManager;
import org.example.user_management.UserRepository;

public class App {
    public static void main(String[] args) {
        Path storagePath = Paths.get("data", "users.db");
        Path scrollStoragePath = Paths.get("data", "scrolls", "scrolls.db");
        Path uploadDirectory = Paths.get("data", "uploads");
        UserRepository userRepository = new UserRepository(storagePath);
        PasswordHasher hasher = new PasswordHasher();
        UserManager userManager = new UserManager(userRepository, hasher);
        DigitalScrollRepository scrollRepository = new DigitalScrollRepository(scrollStoragePath);
        DigitalScrollService scrollService = new DigitalScrollService(scrollRepository, uploadDirectory);
        ScrollSeekerService scrollSeekerService = new ScrollSeekerService(scrollService);
        try (Scanner scanner = new Scanner(System.in)) {
            ScrollSeekerConsole seekerConsole = new ScrollSeekerConsole(scanner, scrollSeekerService);
            UserManagementUI ui = new UserManagementUI(scanner, userManager, scrollService, seekerConsole);
            ui.start();
        }
    }
}
