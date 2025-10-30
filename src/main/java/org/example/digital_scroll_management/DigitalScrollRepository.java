package org.example.digital_scroll_management;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DigitalScrollRepository {
    private static final String DELIMITER = "|";

    private final Path storagePath;
    private final Map<String, DigitalScroll> scrolls = new LinkedHashMap<>();
    private int nextId = 1;

    public DigitalScrollRepository(Path storagePath) {
        this.storagePath = storagePath;
        loadFromFile();
    }

    public synchronized List<DigitalScroll> getAll() {
        return new ArrayList<>(scrolls.values());
    }

    public synchronized DigitalScroll findById(String id) {
        return scrolls.get(id);
    }

    public synchronized void save(DigitalScroll scroll) {
        scrolls.put(scroll.getScrollId(), scroll);
        persist();
    }

    public synchronized void delete(String id) {
        scrolls.remove(id);
        persist();
    }

    public synchronized String generateId() {
        String id = String.format("SC%04d", nextId);
        nextId++;
        return id;
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
                if (parts.length < 4) {
                    continue;
                }
                String id = parts[0];
                String name = parts[1];
                String owner = parts[2];
                String filePath = parts[3];
                LocalDateTime timestamp = LocalDateTime.now();
                int uploadCount = 0;
                int downloadCount = 0;
                if (parts.length >= 5) {
                    timestamp = parseTimestamp(parts[4]);
                }
                if (parts.length >= 6) {
                    uploadCount = parseInt(parts[5]);
                }
                if (parts.length >= 7) {
                    downloadCount = parseInt(parts[6]);
                }
                DigitalScroll scroll = new DigitalScroll(id, name, owner, filePath, timestamp, uploadCount, downloadCount);
                scrolls.put(id, scroll);
                updateNextId(id);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read scroll data: " + storagePath, e);
        }
    }

    private void persist() {
        try {
            Path parent = storagePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(storagePath, StandardCharsets.UTF_8)) {
                for (DigitalScroll scroll : scrolls.values()) {
                    writer.write(String.join(DELIMITER,
                            safe(scroll.getScrollId()),
                            safe(scroll.getName()),
                            safe(scroll.getOwnerUsername()),
                            safe(scroll.getFilePath()),
                            safe(scroll.getUploadTimestamp().toString()),
                            Integer.toString(scroll.getUploadCount()),
                            Integer.toString(scroll.getDownloadCount())));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save scroll data: " + storagePath, e);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void updateNextId(String id) {
        if (id == null) {
            return;
        }
        String numeric = id.replaceAll("[^0-9]", "");
        if (numeric.isEmpty()) {
            return;
        }
        try {
            int value = Integer.parseInt(numeric);
            if (value >= nextId) {
                nextId = value + 1;
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private LocalDateTime parseTimestamp(String raw) {
        try {
            return LocalDateTime.parse(raw);
        } catch (Exception ex) {
            return LocalDateTime.now();
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
