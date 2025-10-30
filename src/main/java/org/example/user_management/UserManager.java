package org.example.user_management;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class UserManager {
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0461\\d{6,11}$");

    private final UserRepository repository;
    private final PasswordHasher passwordHasher;

    public UserManager(UserRepository repository, PasswordHasher passwordHasher) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
    }

    public void ensureDefaultAdmin() {
        if (repository.hasAdmin()) {
            return;
        }
        String hashedPassword = passwordHasher.hash(DEFAULT_ADMIN_PASSWORD);
        String adminCustomId = "admin";
        if (repository.findByCustomId(adminCustomId) != null) {
            adminCustomId = "admin-" + System.currentTimeMillis();
        }
        User admin = new User(
                DEFAULT_ADMIN_USERNAME,
                hashedPassword,
                "admin@example.com",
                "0000000000",
                "System Administrator",
                adminCustomId,
                UserType.ADMIN);
        repository.save(admin);
    }

    public User registerGeneralUser(String username,
                                    String password,
                                    String email,
                                    String phone,
                                    String fullName,
                                    String customId) {
        return createUser(username, password, email, phone, fullName, customId, UserType.GENERAL);
    }

    public User createUser(String username,
                           String password,
                           String email,
                           String phone,
                           String fullName,
                           String customId,
                           UserType userType) {
        String safeUsername = requireValue(username, "Username");
        String safePassword = requireValue(password, "Password");
        String safeEmail = requireValue(email, "Email");
        if (!isValidEmail(safeEmail)) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        String safePhone = requireValue(phone, "Phone");
        if (!isValidPhone(safePhone)) {
            throw new IllegalArgumentException("Invalid phone number format.");
        }
        String safeFullName = requireValue(fullName, "Full name");
        String safeCustomId = requireValue(customId, "Custom ID");

        if (repository.findByUsername(safeUsername) != null) {
            throw new IllegalArgumentException("Username already exists.");
        }
        User existingById = repository.findByCustomId(safeCustomId);
        if (existingById != null) {
            throw new IllegalArgumentException("Custom ID must be unique.");
        }

        String hashedPassword = passwordHasher.hash(safePassword);
        User newUser = new User(
                safeUsername,
                hashedPassword,
                safeEmail,
                safePhone,
                safeFullName,
                safeCustomId,
                userType);
        repository.save(newUser);
        return newUser;
    }

    public User login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        User found = repository.findByUsername(username.trim());
        if (found == null) {
            return null;
        }
        if (passwordHasher.matches(password, found.getPasswordHash())) {
            return found;
        }
        return null;
    }

    public void updateEmail(User user, String email) {
        String safe = requireValue(email, "Email");
        if (!isValidEmail(safe)) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        user.setEmail(safe);
        repository.save(user);
    }

    public void updateFullName(User user, String fullName) {
        String safe = requireValue(fullName, "Full name");
        user.setFullName(safe);
        repository.save(user);
    }

    public void updatePhone(User user, String phone) {
        String safe = requireValue(phone, "Phone");
        if (!isValidPhone(safe)) {
            throw new IllegalArgumentException("Invalid phone number format. Must be 10 to 15 digits.");
        }
        user.setPhoneNumber(safe);
        repository.save(user);
    }

    public void changePassword(User user, String newPassword) {
        String safe = requireValue(newPassword, "Password");
        user.setPasswordHash(passwordHasher.hash(safe));
        repository.save(user);
    }

    public void updateCustomId(User user, String customId) {
        String safe = requireValue(customId, "Custom ID");
        User existing = repository.findByCustomId(safe);
        if (existing != null && !existing.getUsername().equals(user.getUsername())) {
            throw new IllegalArgumentException("Custom ID must be unique.");
        }
        user.setCustomId(safe);
        repository.save(user);
    }

    public void updateProfilePicture(User user, String profilePicturePath) {
        if (user == null) {
            return;
        }
        if (profilePicturePath == null || profilePicturePath.trim().isEmpty()) {
            user.setProfilePicturePath(null);
        } else {
            String trimmed = profilePicturePath.trim();
            if (trimmed.contains("|")) {
                throw new IllegalArgumentException("Profile picture path cannot contain the '|' character.");
            }
            user.setProfilePicturePath(trimmed);
        }
        repository.save(user);
    }

    public List<User> getAllUsers() {
        return repository.getAllUsers();
    }

    public void deleteUser(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username is required.");
        }
        User target = repository.findByUsername(username.trim());
        if (target == null) {
            throw new IllegalArgumentException("User not found.");
        }
        if (target.getUserType() == UserType.ADMIN) {
            throw new IllegalArgumentException("Cannot delete admin users.");
        }
        repository.delete(target.getUsername());
    }

    public User createGuestUser() {
        String guestId = "guest-" + UUID.randomUUID().toString().substring(0, 8);
        return new User(
                "guest",
                "",
                "",
                "",
                "Guest User",
                guestId,
                UserType.GUEST);
    }

    private String requireValue(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        if (trimmed.contains("|")) {
            throw new IllegalArgumentException(fieldName + " cannot contain the '|' character.");
        }
        return trimmed;
    }

    private boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isValidPhone(String phone) {
        if (phone == null) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }
}
