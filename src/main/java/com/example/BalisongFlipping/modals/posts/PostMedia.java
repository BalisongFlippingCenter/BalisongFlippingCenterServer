package com.example.BalisongFlipping.modals.posts;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PostMedia {

    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(name = "is_video")
    private boolean isVideo;

    @Column(columnDefinition = "TEXT")
    private String description;

    public PostMedia() {}

    public PostMedia(String url, boolean isVideo) {
        this.url = url;
        this.isVideo = isVideo;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public boolean isVideo() { return isVideo; }
    public void setIsVideo(boolean isVideo) { this.isVideo = isVideo; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
