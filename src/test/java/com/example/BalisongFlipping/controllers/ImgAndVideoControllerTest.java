package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.FileDto;
import com.example.BalisongFlipping.services.ImgAndVideoService;
import com.example.BalisongFlipping.services.JwtService;
import com.example.BalisongFlipping.services.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImgAndVideoController.class)
@AutoConfigureMockMvc(addFilters = false)
class ImgAndVideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImgAndVideoService imgAndVideoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    void getFileReturnsFileBytesWithContentTypeAndDisposition() throws Exception {
        FileDto fileDto = new FileDto("knife.png", "image/png", "1024", new byte[]{1, 2, 3});
        when(imgAndVideoService.getFile("abc123")).thenReturn(fileDto);

        mockMvc.perform(get("/file/{id}", "abc123"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"knife.png\""));
    }

    @Test
    void getFileReturnsConflictWhenLookupFails() throws Exception {
        when(imgAndVideoService.getFile("missing")).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/file/{id}", "missing"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Failed to get image data"));
    }
}
