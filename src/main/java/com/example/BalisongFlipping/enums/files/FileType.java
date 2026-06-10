package com.example.BalisongFlipping.enums.files;

import org.springframework.http.MediaType;

import java.util.Arrays;

public enum FileType {
    JPG("jpg", MediaType.IMAGE_JPEG),
    JPEG("jpeg", MediaType.IMAGE_JPEG),
    TXT("txt", MediaType.TEXT_PLAIN),
    PNG("png", MediaType.IMAGE_PNG),
    PDF("pdf", MediaType.APPLICATION_PDF);

    private final String extension;
    private final MediaType mediaType;

    FileType(String extension, MediaType mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String getExtension() { return extension; }
    public MediaType getMediaType() { return mediaType; }

    public static MediaType fromFilename(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        String fileExtension = (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
        return Arrays.stream(values())
                .filter(e -> e.getExtension().equals(fileExtension))
                .findFirst()
                .map(FileType::getMediaType)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }
}
