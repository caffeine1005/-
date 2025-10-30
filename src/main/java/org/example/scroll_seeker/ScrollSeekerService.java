package org.example.scroll_seeker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.example.digital_scroll_management.DigitalScroll;
import org.example.digital_scroll_management.DigitalScrollService;

public class ScrollSeekerService {
    private static final int PREVIEW_BYTES = 256;

    private final DigitalScrollService digitalScrollService;

    public ScrollSeekerService(DigitalScrollService digitalScrollService) {
        this.digitalScrollService = digitalScrollService;
    }

    public List<DigitalScroll> filterScrolls(String uploaderFilter,
                                             String scrollIdFilter,
                                             String nameFilter,
                                             LocalDate dateFilter) {
        List<DigitalScroll> all = digitalScrollService.listAllScrolls();
        List<DigitalScroll> result = new ArrayList<>();
        for (DigitalScroll scroll : all) {
            if (!matches(scroll, uploaderFilter, scrollIdFilter, nameFilter, dateFilter)) {
                continue;
            }
            result.add(scroll);
        }
        return result;
    }

    public DigitalScroll findScroll(String scrollId) {
        return digitalScrollService.getScroll(scrollId);
    }

    public ScrollPreview buildPreview(DigitalScroll scroll) {
        Path path = Path.of(scroll.getFilePath());
        long size = fileSize(path);
        String summary = "ID: " + scroll.getScrollId()
                + "\nName: " + scroll.getName()
                + "\nOwner: " + scroll.getOwnerUsername()
                + "\nUploaded: " + scroll.getUploadTimestamp()
                + "\nFile: " + scroll.getFilePath()
                + "\nSize: " + size + " bytes";
        String hexSample = readHexSample(path, PREVIEW_BYTES);
        return new ScrollPreview(summary, hexSample);
    }

    public void downloadScroll(DigitalScroll scroll, Path targetFile) {
        Path source = Path.of(scroll.getFilePath());
        try {
            Files.createDirectories(targetFile.getParent());
            Files.copy(source, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to download scroll to " + targetFile, e);
        }
        digitalScrollService.recordDownload(scroll);
    }

    private boolean matches(DigitalScroll scroll,
                            String uploaderFilter,
                            String scrollIdFilter,
                            String nameFilter,
                            LocalDate dateFilter) {
        if (uploaderFilter != null && !uploaderFilter.isEmpty()) {
            if (!scroll.getOwnerUsername().toLowerCase().contains(uploaderFilter.toLowerCase())) {
                return false;
            }
        }
        if (scrollIdFilter != null && !scrollIdFilter.isEmpty()) {
            if (!scroll.getScrollId().toLowerCase().contains(scrollIdFilter.toLowerCase())) {
                return false;
            }
        }
        if (nameFilter != null && !nameFilter.isEmpty()) {
            if (!scroll.getName().toLowerCase().contains(nameFilter.toLowerCase())) {
                return false;
            }
        }
        if (dateFilter != null) {
            LocalDateTime uploaded = scroll.getUploadTimestamp();
            if (!uploaded.toLocalDate().equals(dateFilter)) {
                return false;
            }
        }
        return true;
    }

    private long fileSize(Path path) {
        try {
            if (Files.exists(path)) {
                return Files.size(path);
            }
        } catch (IOException ignored) {
        }
        return 0;
    }

    private String readHexSample(Path path, int maxBytes) {
        if (!Files.exists(path)) {
            return "(file not found)";
        }
        byte[] buffer = new byte[maxBytes];
        int read = 0;
        try (InputStream input = Files.newInputStream(path)) {
            read = input.read(buffer);
        } catch (IOException e) {
            return "(failed to read file)";
        }
        if (read <= 0) {
            return "(file is empty)";
        }
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < read; i++) {
            if (i > 0) {
                if (i % 16 == 0) {
                    hex.append('\n');
                } else {
                    hex.append(' ');
                }
            }
            hex.append(String.format("%02X", buffer[i]));
        }
        return hex.toString();
    }
}
