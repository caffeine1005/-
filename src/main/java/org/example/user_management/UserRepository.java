package org.example.user_management;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {
    private static final String DELIMITER = "|";

    private final Path storagePath;
    private final Map<String, User> usersByUsername = new LinkedHashMap<>();

    public UserRepository(Path storagePath) {
        this.storagePath = storagePath;
        loadFromFile();
    }

    public synchronized List<User> getAllUsers() {
        return new ArrayList<>(usersByUsername.values());
    }

    public synchronized User findByUsername(String username) {
        return usersByUsername.get(username);
    }

    public synchronized User findByCustomId(String customId) {
        if (customId == null) {
            return null;
        }
        for (User user : usersByUsername.values()) {
            if (customId.equalsIgnoreCase(user.getCustomId())) {
                return user;
            }
        }
        return null;
    }

    public synchronized void save(User user) {
        usersByUsername.put(user.getUsername(), user);
        persist();
    }

    public synchronized void delete(String username) {
        usersByUsername.remove(username);
        persist();
    }

    public synchronized boolean hasAdmin() {
        for (User user : usersByUsername.values()) {
            if (user.getUserType() == UserType.ADMIN) {
                return true;
            }
        }
        return false;
    }

    private void loadFromFile() {
        if (!Files.exists(storagePath)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(storagePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length < 6) {
                    continue;
                }
                String username = parts[0];
                String passwordHash = parts[1];
                String email = parts[2];
                String phone = parts[3];
                String fullName = "";
                String customId = "";
                UserType userType = UserType.GENERAL;
                String profilePicturePath = "";
                if (parts.length == 6) {
                    // Old format without full name: username|password|email|phone|customId|type
                    customId = parts[4];
                    userType = parseUserType(parts[5]);
                    fullName = username;
                } else if (parts.length >= 7) {
                    fullName = parts[4];
                    customId = parts[5];
                    userType = parseUserType(parts[6]);
                    if (parts.length >= 8) {
                        profilePicturePath = parts[7];
                    }
                }
                User user = new User(username, passwordHash, email, phone, fullName, customId, userType);
                if (profilePicturePath == null || profilePicturePath.isEmpty()) {
                    user.setProfilePicturePath(null);
                } else {
                    user.setProfilePicturePath(profilePicturePath);
                }
                usersByUsername.put(username, user);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read user data: " + storagePath, e);
        }
    }

    private void persist() {
        try {
            Path parent = storagePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(storagePath, StandardCharsets.UTF_8)) {
                for (User user : usersByUsername.values()) {
                    writer.write(String.join(DELIMITER,
                            safe(user.getUsername()),
                            safe(user.getPasswordHash()),
                            safe(user.getEmail()),
                            safe(user.getPhoneNumber()),
                            safe(user.getFullName()),
                            safe(user.getCustomId()),
                            user.getUserType().name(),
                            safe(user.getProfilePicturePath())));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save user data: " + storagePath, e);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private UserType parseUserType(String text) {
        if (text == null) {
            return UserType.GENERAL;
        }
        try {
            return UserType.valueOf(text);
        } catch (IllegalArgumentException ignored) {
            return UserType.GENERAL;
        }
    }
}
