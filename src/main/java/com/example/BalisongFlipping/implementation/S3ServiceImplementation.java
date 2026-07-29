package com.example.BalisongFlipping.implementation;

import com.example.BalisongFlipping.services.S3Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class S3ServiceImplementation implements S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3ServiceImplementation.class);

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Presigner s3Presigner;

    @Override
    public void uploadFile(
            final String bucketName,
            final String keyName,
            final Long contentLength,
            final String contentType,
            final InputStream value) throws SdkException {

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .contentLength(contentLength)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(value, contentLength));
        
    }

    @Override
    public String generatePresignedUploadUrl(
            final String bucketName,
            final String keyName,
            final String contentType,
            final Duration expiry) throws SdkException {

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    @Override
    public boolean doesObjectExist(
            final String bucketName,
            final String keyName) throws SdkException {

        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(keyName).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public ByteArrayOutputStream downloadFile(
            final String bucketName,
            final String keyName) throws IOException, SdkException {

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .build();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (InputStream inputStream = s3Client.getObject(request)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer, 0, buffer.length)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        }

        log.info("File downloaded from bucket({}): {}", bucketName, keyName);
        return outputStream;
    }

    @Override
    public List<String> listFiles(final String bucketName) throws SdkException {
        List<String> keys = new ArrayList<>();

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();

        ListObjectsV2Response response;
        do {
            response = s3Client.listObjectsV2(request);
            response.contents().stream()
                    .filter(item -> !item.key().endsWith("/"))
                    .map(S3Object::key)
                    .forEach(keys::add);

            request = request.toBuilder()
                    .continuationToken(response.nextContinuationToken())
                    .build();
        } while (response.isTruncated());

        log.info("Files found in bucket({}): {}", bucketName, keys);
        return keys;
    }

    @Override
    public void deleteFile(
            final String bucketName,
            final String keyName) throws SdkException {

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .build();

        s3Client.deleteObject(request);
        log.info("File deleted from bucket({}): {}", bucketName, keyName);
    }
}