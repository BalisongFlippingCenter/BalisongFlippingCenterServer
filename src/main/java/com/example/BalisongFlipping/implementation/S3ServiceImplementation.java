package com.example.BalisongFlipping.implementation;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.example.BalisongFlipping.services.S3Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class S3ServiceImplementation implements S3Service {
    @Autowired
    private AmazonS3 s3Client;

    /**
     * 
     * @param bucketName
     * @param keyName
     * @param contentLength
     * @param contentType
     * @param value
     * @throws AmazonClientException
     * 
     *                               Method takes in multipart file as input stream,
     *                               uploads it to S3 bucket and returns with s3 put
     *                               object response
     */
    @Override
    public void uploadFile(
            final String bucketName,
            final String keyName,
            final Long contentLength,
            final String contentType,
            final InputStream value) throws AmazonClientException {
        // setting meta data
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        metadata.setContentType(contentType);

        // upload to s3 client
        PutObjectResult res = s3Client.putObject(bucketName, keyName, value, metadata);

        // handle response object

        // log info
        log.info("File uploaded to bucket({}): {}", bucketName, keyName);
    }

    // Method to download a file from an S3 bucket
    @Override
    public ByteArrayOutputStream downloadFile(
            final String bucketName,
            final String keyName) throws IOException, AmazonClientException {
        S3Object s3Object = s3Client.getObject(bucketName, keyName);
        InputStream inputStream = s3Object.getObjectContent();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int len;
        byte[] buffer = new byte[4096];
        while ((len = inputStream.read(buffer, 0, buffer.length)) != -1) {
            outputStream.write(buffer, 0, len);
        }

        log.info("File downloaded from bucket({}): {}", bucketName, keyName);
        return outputStream;
    }

    // Method to list files in an S3 bucket
    @Override
    public List<String> listFiles(final String bucketName) throws AmazonClientException {
        List<String> keys = new ArrayList<>();
        ObjectListing objectListing = s3Client.listObjects(bucketName);

        while (true) {
            List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
            if (objectSummaries.isEmpty()) {
                break;
            }

            objectSummaries.stream()
                    .filter(item -> !item.getKey().endsWith("/"))
                    .map(S3ObjectSummary::getKey)
                    .forEach(keys::add);

            objectListing = s3Client.listNextBatchOfObjects(objectListing);
        }

        log.info("Files found in bucket({}): {}", bucketName, keys);
        return keys;
    }

    // Method to delete a file from an S3 bucket
    @Override
    public void deleteFile(
            final String bucketName,
            final String keyName) throws AmazonClientException {
        s3Client.deleteObject(bucketName, keyName);
        log.info("File deleted from bucket({}): {}", bucketName, keyName);
    }
}
