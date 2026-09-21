package com.example.BalisongFlipping.implementation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ServiceImplementationTest {

    @Mock private S3Client s3Client;
    @Mock private S3Presigner s3Presigner;

    private S3ServiceImplementation s3Service;

    @BeforeEach
    void setUp() {
        s3Service = new S3ServiceImplementation();
        ReflectionTestUtils.setField(s3Service, "s3Client", s3Client);
        ReflectionTestUtils.setField(s3Service, "s3Presigner", s3Presigner);
    }

    @Test
    void uploadFileSendsCorrectBucketKeyAndContentType() {
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);

        s3Service.uploadFile("test-bucket", "posts/1/cover.png", 3L, "image/png", new ByteArrayInputStream(new byte[]{1, 2, 3}));

        verify(s3Client).putObject(captor.capture(), any(software.amazon.awssdk.core.sync.RequestBody.class));
        assertEquals("test-bucket", captor.getValue().bucket());
        assertEquals("posts/1/cover.png", captor.getValue().key());
        assertEquals("image/png", captor.getValue().contentType());
    }

    @Test
    void generatePresignedUploadUrlReturnsUrlFromPresigner() throws Exception {
        PresignedPutObjectRequest presigned = org.mockito.Mockito.mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(new URI("https://test-bucket.s3.amazonaws.com/key?signed").toURL());
        when(s3Presigner.presignPutObject(any(software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest.class)))
                .thenReturn(presigned);

        String url = s3Service.generatePresignedUploadUrl("test-bucket", "posts/1/cover.png", "image/png", Duration.ofMinutes(10));

        assertEquals("https://test-bucket.s3.amazonaws.com/key?signed", url);
    }

    @Test
    void doesObjectExistReturnsTrueWhenHeadObjectSucceeds() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(software.amazon.awssdk.services.s3.model.HeadObjectResponse.builder().build());

        assertTrue(s3Service.doesObjectExist("test-bucket", "posts/1/cover.png"));
    }

    @Test
    void doesObjectExistReturnsFalseWhenKeyMissing() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());

        assertFalse(s3Service.doesObjectExist("test-bucket", "posts/1/missing.png"));
    }

    @Test
    void downloadFileReadsInputStreamIntoOutputStream() throws Exception {
        byte[] content = "hello world".getBytes();
        software.amazon.awssdk.core.ResponseInputStream<software.amazon.awssdk.services.s3.model.GetObjectResponse> responseStream =
                new software.amazon.awssdk.core.ResponseInputStream<>(
                        software.amazon.awssdk.services.s3.model.GetObjectResponse.builder().build(),
                        software.amazon.awssdk.http.AbortableInputStream.create(new ByteArrayInputStream(content)));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

        var result = s3Service.downloadFile("test-bucket", "posts/1/file.txt");

        assertEquals("hello world", result.toString());
    }

    @Test
    void listFilesFiltersOutFolderMarkersAndPagesThroughResults() {
        ListObjectsV2Response page1 = ListObjectsV2Response.builder()
                .contents(
                        S3Object.builder().key("posts/1/").build(),
                        S3Object.builder().key("posts/1/a.png").build())
                .isTruncated(true)
                .nextContinuationToken("token-2")
                .build();
        ListObjectsV2Response page2 = ListObjectsV2Response.builder()
                .contents(S3Object.builder().key("posts/1/b.png").build())
                .isTruncated(false)
                .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(page1, page2);

        List<String> files = s3Service.listFiles("test-bucket");

        assertEquals(List.of("posts/1/a.png", "posts/1/b.png"), files);
    }

    @Test
    void deleteFileSendsCorrectBucketAndKey() {
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);

        s3Service.deleteFile("test-bucket", "posts/1/cover.png");

        verify(s3Client).deleteObject(captor.capture());
        assertEquals("test-bucket", captor.getValue().bucket());
        assertEquals("posts/1/cover.png", captor.getValue().key());
    }
}
