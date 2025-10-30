package org.example.user_management;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import java.io.Console;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.example.digital_scroll_management.DigitalScroll;
import org.example.digital_scroll_management.DigitalScrollService;
import org.example.scroll_seeker.ScrollSeekerConsole;

public class UserManagementUI {
    private final Scanner scanner;
    private final UserManager userManager;
    private final DigitalScrollService scrollService;
    private final ScrollSeekerConsole scrollSeekerConsole;
    private User currentUser;
    private boolean running = true;

    public UserManagementUI(Scanner scanner,
                            UserManager userManager,
                            DigitalScrollService scrollService,
                            ScrollSeekerConsole scrollSeekerConsole) {
        this.scanner = scanner;
        this.userManager = userManager;
        this.scrollService = scrollService;
        this.scrollSeekerConsole = scrollSeekerConsole;
    }

    public void start() {
        userManager.ensureDefaultAdmin();
        System.out.println("Welcome to the Virtual Scroll Access System (VSAS)");
        try {
            while (running) {
                showMainMenu();
            }
        } catch (InputClosedException ex) {
            System.out.println();
            System.out.println("Input stream closed. Exiting application.");
        }
    }

    private void showMainMenu() {
        System.out.println("--------------------------------------");
        System.out.println("Current user: " + getDisplayName());
        System.out.println("1. Log in");
        System.out.println("2. Register");
        System.out.println("3. Continue as guest");
        System.out.println("4. Exit");
        String choice = prompt("Select an option: ");
        switch (choice) {
            case "1" -> handleLogin();
            case "2" -> handleRegistration();
            case "3" -> handleGuest();
            case "4" -> {
                running = false;
                System.out.println("Goodbye!");
            }
            default -> System.out.println("Invalid option, please try again.");
        }
    }

    private void handleLogin() {
        String username = prompt("Username: ");
        String password = promptPassword("Password: ");
        User loggedIn = userManager.login(username, password);
        if (loggedIn == null) {
            System.out.println("Login failed. Please check your details.");
            return;
        }
        currentUser = loggedIn;
        System.out.println("Welcome, " + currentUser.getUsername() + "!");
        accountMenu();
    }

    private void handleRegistration() {
        try {
            String username = prompt("Choose a username: ");
            String password = promptPassword("Choose a password: ");
            String email = prompt("Email: ");
            String phone = prompt("Phone number: ");
            String fullName = prompt("Full name: ");
            String customId = prompt("Custom ID: ");
            currentUser = userManager.registerGeneralUser(username, password, email, phone, fullName, customId);
            updateProfilePicture();
            System.out.println("Registration successful. You are now logged in.");
            accountMenu();
        } catch (IllegalArgumentException ex) {
            System.out.println("Registration failed: " + ex.getMessage());
        }
    }

    private void handleGuest() {
        currentUser = userManager.createGuestUser();
        System.out.println("Guest mode enabled. Upload and download are disabled.");
        guestMenu();
        currentUser = null;
    }

    private void accountMenu() {
        boolean stay = true;
        while (stay && currentUser != null && currentUser.getUserType() != UserType.GUEST) {
            System.out.println("--------------------------------------");
            System.out.println("Account menu (" + getDisplayName() + ")");
            System.out.println("1. View profile");
            System.out.println("2. Update profile");
            System.out.println("3. Scroll management");
            System.out.println("4. View & download scrolls");
            System.out.println("5. Return");
            String choice = prompt("Select an option: ");
            switch (choice) {
                case "1" -> showProfile();
                case "2" -> updateProfileMenu();
                case "3" -> scrollManagementMenu();
                case "4" -> scrollSeekerConsole.viewAndDownloadMenu(true);
                case "5" -> {
                    System.out.println("Returning to main menu.");
                    stay = false;
                }
                default -> System.out.println("Invalid option, please try again.");
            }
        }
        if (currentUser != null && currentUser.getUserType() != UserType.GUEST) {
            System.out.println("You have been logged out.");
            currentUser = null;
        }
    }

    private void updateProfileMenu() {
        if (currentUser == null) {
            return;
        }
        boolean stay = true;
        while (stay) {
            System.out.println("--------------------------------------");
            System.out.println("Update profile (" + getDisplayName() + ")");
            System.out.println("1. Update full name");
            System.out.println("2. Update email");
            System.out.println("3. Update phone");
            System.out.println("4. Change password");
            System.out.println("5. Update custom ID");
            System.out.println("6. Update profile picture");
            System.out.println("7. Return");
            String choice = prompt("Select an option: ");
            switch (choice) {
                case "1" -> updateFullName();
                case "2" -> updateEmail();
                case "3" -> updatePhone();
                case "4" -> updatePassword();
                case "5" -> updateCustomId();
                case "6" -> updateProfilePicture();
                case "7" -> stay = false;
                default -> System.out.println("Invalid option, please try again.");
            }
        }
    }

    private void scrollManagementMenu() {
        if (currentUser == null) {
            return;
        }
        if (currentUser.getUserType() == UserType.ADMIN) {
            adminScrollMenu();
        } else {
            userScrollMenu();
        }
    }

    private void userScrollMenu() {
        boolean stay = true;
        while (stay && currentUser != null) {
            System.out.println("--------------------------------------");
            System.out.println("Scroll management (" + getDisplayName() + ")");
            System.out.println("1. List my scrolls");
            System.out.println("2. Add new scroll");
            System.out.println("3. Edit my scroll");
            System.out.println("4. Remove my scroll");
            System.out.println("5. Return");
            String choice = prompt("Select an option: ");
            switch (choice) {
                case "1" -> listMyScrolls();
                case "2" -> addScroll();
                case "3" -> editScroll();
                case "4" -> removeScroll();
                case "5" -> stay = false;
                default -> System.out.println("Invalid option, please try again.");
            }
        }
    }

    private void adminScrollMenu() {
        boolean stay = true;
        while (stay && currentUser != null) {
            System.out.println("--------------------------------------");
            System.out.println("Admin scroll management (" + getDisplayName() + ")");
            System.out.println("1. List all scrolls");
            System.out.println("2. List my scrolls");
            System.out.println("3. Add new scroll");
            System.out.println("4. Edit my scroll");
            System.out.println("5. Remove my scroll");
            System.out.println("6. List all users");
            System.out.println("7. Create user");
            System.out.println("8. Delete user");
            System.out.println("9. View scroll stats");
            System.out.println("10. Return");
            String choice = prompt("Select an option: ");
            switch (choice) {
                case "1" -> listAllScrolls();
                case "2" -> listMyScrolls();
                case "3" -> addScroll();
                case "4" -> editScroll();
                case "5" -> removeScroll();
                case "6" -> listUsers();
                case "7" -> createUserByAdmin();
                case "8" -> deleteUserByAdmin();
                case "9" -> showStats();
                case "10" -> stay = false;
                default -> System.out.println("Invalid option, please try again.");
            }
        }
    }

    private void guestMenu() {
        boolean stay = true;
        while (stay && currentUser != null && currentUser.getUserType() == UserType.GUEST) {
            System.out.println("--------------------------------------");
            System.out.println("Guest menu (" + getDisplayName() + ")");
            System.out.println("1. Browse scrolls");
            System.out.println("2. Return to main menu");
            String choice = prompt("Select an option: ");
            switch (choice) {
                case "1" -> scrollSeekerConsole.viewAndDownloadMenu(false);
                case "2" -> stay = false;
                default -> System.out.println("Invalid option, please try again.");
            }
        }
    }

    private void showProfile() {
        if (currentUser == null) {
            return;
        }
        System.out.println("Username: " + currentUser.getUsername());
        System.out.println("Full name: " + currentUser.getFullName());
        System.out.println("Email: " + currentUser.getEmail());
        System.out.println("Phone: " + currentUser.getPhoneNumber());
        System.out.println("Custom ID: " + currentUser.getCustomId());
        System.out.println("User type: " + currentUser.getUserType().name().toLowerCase());
        String picturePath = currentUser.getProfilePicturePath();
        if (picturePath == null || picturePath.isBlank()) {
            System.out.println("Profile picture: not set");
        } else {
            System.out.println("Profile picture: " + picturePath);
        }
    }

    private void updateFullName() {
        if (currentUser == null) {
            return;
        }
        try {
            String fullName = prompt("New full name: ");
            userManager.updateFullName(currentUser, fullName);
            System.out.println("Full name updated.");
        } catch (IllegalArgumentException ex) {
            System.out.println("Update failed: " + ex.getMessage());
        }
    }

    private void updateEmail() {
        if (currentUser == null) {
            return;
        }
        try {
            String email = prompt("New email: ");
            userManager.updateEmail(currentUser, email);
            System.out.println("Email updated.");
        } catch (IllegalArgumentException ex) {
            System.out.println("Update failed: " + ex.getMessage());
        }
    }

    private void updatePhone() {
        if (currentUser == null) {
            return;
        }
        try {
            String phone = prompt("New phone: ");
            userManager.updatePhone(currentUser, phone);
            System.out.println("Phone updated.");
        } catch (IllegalArgumentException ex) {
            System.out.println("Update failed: " + ex.getMessage());
        }
    }

    private void updatePassword() {
        if (currentUser == null) {
            return;
        }
        try {
            String password = prompt("New password: ");
            userManager.changePassword(currentUser, password);
            System.out.println("Password updated.");
        } catch (IllegalArgumentException ex) {
            System.out.println("Update failed: " + ex.getMessage());
        }
    }

    private void updateCustomId() {
        if (currentUser == null) {
            return;
        }
        try {
            String customId = prompt("New custom ID: ");
            userManager.updateCustomId(currentUser, customId);
            System.out.println("Custom ID updated.");
        } catch (IllegalArgumentException ex) {
            System.out.println("Update failed: " + ex.getMessage());
        }
    }

    private void updateProfilePicture() {
        if (currentUser == null) {
            return;
        }
        String response = prompt("Do you want to add or update a profile picture? (yes/no): ");
        if (!(response.equalsIgnoreCase("yes") || response.equalsIgnoreCase("y"))) {
            System.out.println("Profile picture update cancelled.");
            return;
        }
        String selectedPath = chooseFile("Select Profile Picture", "Save");
        if (selectedPath == null) {
            return;
        }
        try {
            Path source = Paths.get(selectedPath);
            if (!Files.exists(source) || Files.isDirectory(source)) {
                System.out.println("Selected file is not valid.");
                return;
            }
            Path storageDir = Paths.get("data", "profile_pictures");
            Files.createDirectories(storageDir);
            String fileName = source.getFileName().toString();
            String extension = "";
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = fileName.substring(dotIndex);
            }
            String sanitizedBase = currentUser.getUsername().replaceAll("[^a-zA-Z0-9-_]", "_");
            if (sanitizedBase.isEmpty()) {
                sanitizedBase = "user";
            }
            Path target = storageDir.resolve(sanitizedBase + extension);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            userManager.updateProfilePicture(currentUser, target.toAbsolutePath().toString());
            System.out.println("Profile picture saved.");
        } catch (InvalidPathException ex) {
            System.out.println("Selected path is invalid: " + ex.getReason());
        } catch (IOException ex) {
            System.out.println("Failed to save profile picture: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            System.out.println("Update failed: " + ex.getMessage());
        }
    }

    private void listUsers() {
        List<User> users = userManager.getAllUsers();
        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }
        System.out.println("All users:");
        for (User user : users) {
            System.out.println(" - " + formatUserLine(user));
        }
    }

    private void createUserByAdmin() {
        try {
            String username = prompt("Username: ");
            String password = prompt("Password: ");
            String email = prompt("Email: ");
            String phone = prompt("Phone number: ");
            String fullName = prompt("Full name: ");
            String customId = prompt("Custom ID: ");
            UserType type = readUserType();
            userManager.createUser(username, password, email, phone, fullName, customId, type);
            System.out.println("User created.");
        } catch (IllegalArgumentException ex) {
            System.out.println("Creation failed: " + ex.getMessage());
        }
    }

    private void deleteUserByAdmin() {
        String username = prompt("Username to delete: ");
        try {
            userManager.deleteUser(username);
            System.out.println("User deleted.");
        } catch (IllegalArgumentException ex) {
            System.out.println("Delete failed: " + ex.getMessage());
        }
    }

    private void showStats() {
        List<String> stats = scrollService.getScrollStatistics();
        System.out.println("Scroll stats:");
        for (String line : stats) {
            System.out.println(" - " + line);
        }
    }

    private void listMyScrolls() {
        if (currentUser == null) {
            return;
        }
        List<DigitalScroll> scrolls = scrollService.listScrollsByOwner(currentUser.getUsername());
        if (scrolls.isEmpty()) {
            System.out.println("You do not have any scrolls yet.");
            return;
        }
        System.out.println("Your scrolls:");
        for (DigitalScroll scroll : scrolls) {
            System.out.println(" - " + formatScrollLine(scroll));
        }
    }

    private void addScroll() {
        if (currentUser == null) {
            return;
        }
        try {
            String name = prompt("Scroll name: ");
            String selectedPath = chooseFile();
            if (selectedPath == null) {
                return;
            }
            DigitalScroll scroll = scrollService.addScroll(currentUser.getUsername(), name, selectedPath);
            System.out.println("Successful upload. Scroll ID: " + scroll.getScrollId() + ".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Add failed: " + ex.getMessage());
        }
    }

    private void editScroll() {
        if (currentUser == null) {
            return;
        }
        String id = prompt("Scroll ID to edit: ");
        String newName = prompt("New name (leave blank to keep current): ");
        String replace = prompt("Replace binary file? (y/n): ");
        String newFile = null;
        if (replace.equalsIgnoreCase("y")) {
            newFile = chooseFile();
            if (newFile == null) {
                return;
            }
        }
        try {
            scrollService.updateScroll(currentUser.getUsername(), id, newName, newFile);
            System.out.println("Scroll updated.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Update failed: " + ex.getMessage());
        }
    }

    private void removeScroll() {
        if (currentUser == null) {
            return;
        }
        String id = prompt("Scroll ID to remove: ");
        try {
            scrollService.removeScroll(currentUser.getUsername(), id);
            System.out.println("Scroll removed.");
        } catch (IllegalArgumentException ex) {
            System.out.println("Remove failed: " + ex.getMessage());
        }
    }

    private void listAllScrolls() {
        List<DigitalScroll> scrolls = scrollService.listAllScrolls();
        if (scrolls.isEmpty()) {
            System.out.println("There are no scrolls in the library.");
            return;
        }
        System.out.println("All scrolls:");
        for (DigitalScroll scroll : scrolls) {
            System.out.println(" - " + formatScrollLine(scroll));
        }
    }

    private UserType readUserType() {
        while (true) {
            String choice = prompt("Select user type (1 = General, 2 = Admin): ");
            if ("1".equals(choice)) {
                return UserType.GENERAL;
            }
            if ("2".equals(choice)) {
                return UserType.ADMIN;
            }
            System.out.println("Invalid option, please try again.");
        }
    }

    private String formatUserLine(User user) {
        return "username=" + user.getUsername()
                + " fullName=" + user.getFullName()
                + " type=" + user.getUserType().name().toLowerCase()
                + " email=" + user.getEmail()
                + " phone=" + user.getPhoneNumber()
                + " id=" + user.getCustomId();
    }

    private String formatScrollLine(DigitalScroll scroll) {
        return "id=" + scroll.getScrollId()
                + " name=" + scroll.getName()
                + " owner=" + scroll.getOwnerUsername()
                + " uploaded=" + scroll.getUploadTimestamp()
                + " file=" + scroll.getFilePath();
    }

    private String getDisplayName() {
        if (currentUser == null) {
            return "Not logged in";
        }
        String typeLabel = currentUser.getUserType().name().toLowerCase();
        String displayName = currentUser.getFullName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = currentUser.getUsername();
        }
        return displayName + " (" + typeLabel + ")";
    }

    private String prompt(String message) {
        System.out.print(message);
        try {
            String line = scanner.nextLine();
            if (line == null) {
                throw new InputClosedException();
            }
            return line.trim();
        } catch (IllegalStateException | java.util.NoSuchElementException ex) {
            throw new InputClosedException();
        }
    }

    private static class InputClosedException extends RuntimeException {
        InputClosedException() {
            super("scanner closed");
        }
    }

    private String chooseFile() {
        return chooseFile("Select Scroll File", "Upload");
    }

    private String chooseFile(String dialogTitle, String approveLabel) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("GUI file chooser is not available. Please type the file path.");
            String manual = prompt("File path (leave blank to cancel): ");
            if (manual.isEmpty()) {
                System.out.println("File selection cancelled.");
                return null;
            }
            return manual;
        }
        final String[] selectedPath = new String[1];
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame(dialogTitle);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                JTextField pathField = new JTextField(25);
                JButton browseButton = new JButton("Browse...");
                JButton okButton = new JButton(approveLabel);
                JButton cancelButton = new JButton("Cancel");

                browseButton.addActionListener(e -> {
                    JFileChooser chooser = new JFileChooser();
                    int result = chooser.showOpenDialog(frame);
                    if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                        pathField.setText(chooser.getSelectedFile().getAbsolutePath());
                    }
                });

                okButton.addActionListener(e -> {
                    selectedPath[0] = pathField.getText().trim();
                    frame.dispose();
                    latch.countDown();
                });

                cancelButton.addActionListener(e -> {
                    selectedPath[0] = null;
                    frame.dispose();
                    latch.countDown();
                });

                JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                inputPanel.add(new JLabel("File:"));
                inputPanel.add(pathField);
                inputPanel.add(browseButton);

                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                buttonPanel.add(okButton);
                buttonPanel.add(cancelButton);

                frame.getContentPane().add(inputPanel, BorderLayout.CENTER);
                frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            });
            latch.await();
        } catch (HeadlessException ex) {
            System.out.println("File chooser is not available in this environment.");
            return null;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            System.out.println("File selection interrupted.");
            return null;
        }
        if (selectedPath[0] == null || selectedPath[0].isEmpty()) {
            System.out.println("File selection cancelled.");
            return null;
        }
        return selectedPath[0];
    }

    private String promptPassword(String message) {
        Console console = System.console();
        if (console != null) {
            console.format("%s", message);
            console.flush();
            char[] pwd = console.readPassword();
            return pwd == null ? "" : new String(pwd);
        }

        if (!GraphicsEnvironment.isHeadless()) {
            final String[] result = new String[1];
            final CountDownLatch latch = new CountDownLatch(1);
            SwingUtilities.invokeLater(() -> {
                JPasswordField pf = new JPasswordField(25);
                JPanel panel = new JPanel(new BorderLayout(5, 5));
                panel.add(new JLabel(message), BorderLayout.WEST);
                panel.add(pf, BorderLayout.CENTER);
                int option = JOptionPane.showConfirmDialog(
                        null,
                        panel,
                        "Enter Password",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );
                if (option == JOptionPane.OK_OPTION) {
                    result[0] = new String(pf.getPassword());
                } else {
                    result[0] = "";
                }
                latch.countDown();
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "";
            }
            return result[0] == null ? "" : result[0];
        }

        System.out.println("(Warning) Cannot hide input in this environment.");
        return prompt(message);
    }
}
