package org.example.scroll_seeker;

public class ScrollPreview {
    private final String summary;
    private final String hexSample;

    public ScrollPreview(String summary, String hexSample) {
        this.summary = summary;
        this.hexSample = hexSample;
    }

    public String getSummary() {
        return summary;
    }

    public String getHexSample() {
        return hexSample;
    }
}
