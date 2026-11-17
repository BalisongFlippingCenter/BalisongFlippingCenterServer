package com.example.BalisongFlipping.services;

import software.amazon.awssdk.core.exception.SdkException;

import java.io.InputStream;
import java.time.Duration;

public interface S3Service {
    // Method to upload a file to an S3 bucket
    void uploadFile(
        final String bucketName,
        final String keyName,
        final Long contentLength,
        final String contentType,
        final InputStream value
    ) throws SdkException;

    // Method to generate a presigned URL a client can PUT a file to directly
    String generatePresignedUploadUrl(
        final String bucketName,
        final String keyName,
        final String contentType,
        final Duration expiry
    ) throws SdkException;

    // Method to check whether an object exists in an S3 bucket
    boolean doesObjectExist(
        final String bucketName,
        final String keyName
    ) throws SdkException;
}
