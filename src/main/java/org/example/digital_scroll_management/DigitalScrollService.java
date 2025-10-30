package org.example.digital_scroll_management;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DigitalScrollService {
    private final DigitalScrollRepository repository;
    private final Path uploadDirectory;

    public DigitalScrollService(DigitalScrollRepository repository, Path uploadDirectory) {
        this.repository = repository;
        this.uploadDirectory = uploadDirectory;
        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create upload directory: " + uploadDirectory, e);
        }
    }

    public List<DigitalScroll> listAllScrolls() {
        return repository.getAll();
    }

    public List<DigitalScroll> listScrollsByOwner(String ownerUsername) {
        List<DigitalScroll> result = new ArrayList<>();
        for (DigitalScroll scroll : repository.getAll()) {
            if (scroll.getOwnerUsername().equals(ownerUsername)) {
                result.add(scroll);
            }
        }
        return result;
    }

    public DigitalScroll addScroll(String ownerUsername, String name, String sourceFilePath) {
        String safeName = requireValue(name, "Scroll name");
        ensureUniqueName(safeName, null);
        Path source = checkReadableFile(sourceFilePath);
        String scrollId = repository.generateId();
        Path target = buildTargetPath(scrollId, safeName, source);
        copyFile(source, target);
        DigitalScroll scroll = new DigitalScroll(scrollId, safeName, ownerUsername, target.toString(), LocalDateTime.now(), 1, 0);
        repository.save(scroll);
        return scroll;
    }

    public void updateScroll(String ownerUsername,
                             String scrollId,
                             String newName,
                             String newSourceFilePath) {
        DigitalScroll scroll = requireOwnedScroll(ownerUsername, scrollId);
        boolean fileReplaced = false;
        if (newName != null && !newName.trim().isEmpty()) {
            String safeName = newName.trim();
            ensureUniqueName(safeName, scrollId);
            scroll.setName(safeName);
        }
        if (newSourceFilePath != null && !newSourceFilePath.trim().isEmpty()) {
            Path source = checkReadableFile(newSourceFilePath);
            Path target = Path.of(scroll.getFilePath());
            copyFile(source, target);
            fileReplaced = true;
        }
        if (fileReplaced) {
            scroll.incrementUploadCount();
        }
        repository.save(scroll);
    }

    public DigitalScroll getScroll(String scrollId) {
        if (scrollId == null) {
            return null;
        }
        return repository.findById(scrollId.trim());
    }

    public void removeScroll(String ownerUsername, String scrollId) {
        DigitalScroll scroll = requireOwnedScroll(ownerUsername, scrollId);
        Path filePath = Path.of(scroll.getFilePath());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }
        repository.delete(scrollId);
    }

    public void recordDownload(DigitalScroll scroll) {
        scroll.incrementDownloadCount();
        repository.save(scroll);
    }

    public List<String> getScrollStatistics() {
        List<String> stats = new ArrayList<>();
        for (DigitalScroll scroll : repository.getAll()) {
            stats.add(String.format("id=%s name=%s uploads=%d downloads=%d",
                    scroll.getScrollId(),
                    scroll.getName(),
                    scroll.getUploadCount(),
                    scroll.getDownloadCount()));
        }
        return stats;
    }

    private DigitalScroll requireOwnedScroll(String ownerUsername, String scrollId) {
        if (scrollId == null || scrollId.trim().isEmpty()) {
            throw new IllegalArgumentException("Scroll ID is required.");
        }
        DigitalScroll scroll = repository.findById(scrollId.trim());
        if (scroll == null) {
            throw new IllegalArgumentException("Scroll not found.");
        }
        if (!scroll.getOwnerUsername().equals(ownerUsername)) {
            throw new IllegalArgumentException("You can only modify your own scrolls.");
        }
        return scroll;
    }

    private void ensureUniqueName(String name, String ignoreScrollId) {
        for (DigitalScroll existing : repository.getAll()) {
            if (existing.getName().equalsIgnoreCase(name)) {
                if (ignoreScrollId == null || !existing.getScrollId().equals(ignoreScrollId)) {
                    throw new IllegalArgumentException("Scroll name must be unique.");
                }
            }
        }
    }

    private Path checkReadableFile(String filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path is required.");
        }
        Path source = Path.of(filePath.trim());
        if (!Files.exists(source) || !Files.isRegularFile(source)) {
            throw new IllegalArgumentException("Source file does not exist.");
        }
        return source;
    }

    private Path buildTargetPath(String scrollId, String name, Path source) {
        String cleanName = name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String extension = extractExtension(source.getFileName().toString());
        String filename = scrollId + "_" + cleanName + extension;
        return uploadDirectory.resolve(filename);
    }

    private String extractExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index >= 0 && index < filename.length() - 1) {
            return filename.substring(index);
        }
        return "";
    }

    private void copyFile(Path source, Path target) {
        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to copy file to " + target, e);
        }
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
}
