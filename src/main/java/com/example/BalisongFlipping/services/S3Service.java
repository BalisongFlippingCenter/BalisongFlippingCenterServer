package com.example.BalisongFlipping.services;

import software.amazon.awssdk.core.exception.SdkException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;

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

    // Method to download a file from an S3 bucket
    ByteArrayOutputStream downloadFile(
        final String bucketName,
        final String keyName
    ) throws IOException, SdkException;

    // Method to list files in an S3 bucket
    List<String> listFiles(final String bucketName) throws SdkException;

    // Method to delete a file from an S3 bucket
    void deleteFile(
        final String bucketName,
        final String keyName
    ) throws SdkException;
}
