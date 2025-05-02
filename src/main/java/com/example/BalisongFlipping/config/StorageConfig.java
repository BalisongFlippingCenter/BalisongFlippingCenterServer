package com.example.BalisongFlipping.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {
    
    // Injecting IAM access key from application.properties
    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    // Injecting IAM secret key from application.properties
    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    // Injection aws region from applicatoin properties
    @Value("${cloud.aws.region.static}")
    private String region;

    // creating Bean to create and configure aws s3 credentials
    @Bean
    public AmazonS3 s3Client() {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build(); 
    }
}
