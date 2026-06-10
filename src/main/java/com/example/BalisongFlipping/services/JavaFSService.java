package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.FileDto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Placeholder — previously used MongoDB GridFS. Will be replaced by S3Service once
 * S3 file upload is implemented. All callers (CollectionService) reference this until then.
 */
@Service
public class JavaFSService {

    public String addAsset(String title, MultipartFile file) throws Exception {
        throw new UnsupportedOperationException("File storage not yet migrated to S3.");
    }

    public FileDto getAsset(String id) throws Exception {
        throw new UnsupportedOperationException("File storage not yet migrated to S3.");
    }

    public boolean deleteAsset(String id) throws Exception {
        throw new UnsupportedOperationException("File storage not yet migrated to S3.");
    }
}
