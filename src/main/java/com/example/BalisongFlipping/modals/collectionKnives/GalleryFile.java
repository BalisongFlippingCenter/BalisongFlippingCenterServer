package com.example.BalisongFlipping.modals.collectionKnives;

import jakarta.persistence.Embeddable;

@Embeddable
public class GalleryFile {

    private String postId;
    private String fileId;

    public GalleryFile() {}

    public GalleryFile(String fileId, String postId) {
        this.fileId = fileId;
        this.postId = postId;
    }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
}
