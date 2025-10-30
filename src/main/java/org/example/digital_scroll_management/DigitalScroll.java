package org.example.digital_scroll_management;

import java.time.LocalDateTime;

public class DigitalScroll {
    private final String scrollId;
    private final LocalDateTime uploadTimestamp;
    private String name;
    private String ownerUsername;
    private String filePath;
    private int uploadCount;
    private int downloadCount;

    public DigitalScroll(String scrollId,
                         String name,
                         String ownerUsername,
                         String filePath,
                         LocalDateTime uploadTimestamp,
                         int uploadCount,
                         int downloadCount) {
        this.scrollId = scrollId;
        this.name = name;
        this.ownerUsername = ownerUsername;
        this.filePath = filePath;
        this.uploadTimestamp = uploadTimestamp;
        this.uploadCount = uploadCount;
        this.downloadCount = downloadCount;
    }

    public String getScrollId() {
        return scrollId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public LocalDateTime getUploadTimestamp() {
        return uploadTimestamp;
    }

    public int getUploadCount() {
        return uploadCount;
    }

    public void incrementUploadCount() {
        this.uploadCount++;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
    }
}
