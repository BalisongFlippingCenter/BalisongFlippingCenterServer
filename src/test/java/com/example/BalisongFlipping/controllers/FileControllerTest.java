package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.services.JwtService;
import com.example.BalisongFlipping.services.RefreshTokenService;
import com.example.BalisongFlipping.services.S3Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private S3Service service;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    void listFilesReturnsBucketContents() throws Exception {
        when(service.listFiles("knife-images")).thenReturn(List.of("a.png", "b.png"));

        mockMvc.perform(get("/files/{bucketName}", "knife-images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("a.png"))
                .andExpect(jsonPath("$[1]").value("b.png"));
    }

    @Test
    void uploadFileSucceeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "knife.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/files/{bucketName}/upload", "knife-images").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("File uploaded successfully"));

        verify(service).uploadFile(eq("knife-images"), eq("knife.png"), anyLong(), eq("image/png"), any());
    }

    @Test
    void uploadFileRejectsEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/files/{bucketName}/upload", "knife-images").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("File is empty"));

        verify(service, never()).uploadFile(any(), any(), any(), any(), any());
    }

    @Test
    void downloadFileReturnsBytesWithContentDisposition() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(new byte[]{1, 2, 3});
        when(service.downloadFile("knife-images", "knife.png")).thenReturn(stream);

        mockMvc.perform(get("/files/{bucketName}/download/{fileName}", "knife-images", "knife.png"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"knife.png\""));
    }

    @Test
    void deleteFileSucceeds() throws Exception {
        mockMvc.perform(delete("/files/{bucketName}/{fileName}", "knife-images", "knife.png"))
                .andExpect(status().isOk());

        verify(service, times(1)).deleteFile("knife-images", "knife.png");
    }
}
