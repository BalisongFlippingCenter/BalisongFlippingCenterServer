package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.FileDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImgAndVideoServiceTest {

    @Mock private JavaFSService javaFSService;

    private ImgAndVideoService imgAndVideoService;

    @BeforeEach
    void setUp() {
        imgAndVideoService = new ImgAndVideoService(javaFSService);
    }

    @Test
    void getFileDelegatesToJavaFSService() throws Exception {
        FileDto dto = new FileDto("knife.png", "image/png", "1024", new byte[]{1, 2, 3});
        when(javaFSService.getAsset("abc123")).thenReturn(dto);

        assertEquals(dto, imgAndVideoService.getFile("abc123"));
    }

    @Test
    void getFilePropagatesExceptionFromJavaFSService() throws Exception {
        when(javaFSService.getAsset("missing")).thenThrow(new Exception("Not found"));

        assertThrows(Exception.class, () -> imgAndVideoService.getFile("missing"));
    }
}
