package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.catalogDtos.KnifeDetailDto;
import com.example.BalisongFlipping.dtos.catalogSeedDtos.CatalogImageUploadUrlRequestDto;
import com.example.BalisongFlipping.dtos.catalogSeedDtos.CatalogImportDto;
import com.example.BalisongFlipping.dtos.catalogSeedDtos.KnifeSeedDto;
import com.example.BalisongFlipping.dtos.catalogSeedDtos.MakerSeedDto;
import com.example.BalisongFlipping.dtos.uploadsDtos.PresignedUploadTargetDto;
import com.example.BalisongFlipping.modals.knifeCatalog.Maker;
import com.example.BalisongFlipping.seed.CatalogSeedService;
import com.example.BalisongFlipping.seed.CatalogValidationException;
import com.example.BalisongFlipping.services.JwtService;
import com.example.BalisongFlipping.services.KnifeCatalogService;
import com.example.BalisongFlipping.services.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CatalogSeedService catalogSeedService;

    @MockBean
    private KnifeCatalogService knifeCatalogService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    private MakerSeedDto makerSeed() {
        return new MakerSeedDto("squid-industries", "Squid Industries", "USA", "Trainers",
                null, null, 2014, null, null, null, null);
    }

    private Maker maker() {
        Maker maker = new Maker();
        maker.setSlug("squid-industries");
        maker.setName("Squid Industries");
        return maker;
    }

    private KnifeSeedDto knifeSeed() {
        return new KnifeSeedDto("mako", "Mako", "Squid Industries", "squid-industries",
                "Reverse Tanto", "$150-$200", null, "A great trainer.", List.of());
    }

    @Test
    void getImageUploadUrlSucceeds() throws Exception {
        when(catalogSeedService.generateImageUploadUrl(any()))
                .thenReturn(new PresignedUploadTargetDto("catalog/mako/cover.png", "https://upload", "https://public", false));

        mockMvc.perform(post("/admin/catalog/upload-url")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new CatalogImageUploadUrlRequestDto("mako", null, null, "cover.png", "image/png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("catalog/mako/cover.png"));
    }

    @Test
    void getImageUploadUrlRejectsServiceFailure() throws Exception {
        when(catalogSeedService.generateImageUploadUrl(any())).thenThrow(new Exception("Knife not found."));

        mockMvc.perform(post("/admin/catalog/upload-url")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new CatalogImageUploadUrlRequestDto("bogus", null, null, "cover.png", "image/png"))))
                .andExpect(status().isConflict())
                .andExpect(content().string("Knife not found."));
    }

    @Test
    void importCatalogSucceeds() throws Exception {
        mockMvc.perform(post("/admin/catalog/import")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CatalogImportDto(List.of(makerSeed()), List.of()))))
                .andExpect(status().isOk());
    }

    @Test
    void importCatalogRejectsInvalidData() throws Exception {
        doThrow(new CatalogValidationException("Knife references unknown maker slug."))
                .when(catalogSeedService).importCatalog(any(), any());

        mockMvc.perform(post("/admin/catalog/import")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CatalogImportDto(List.of(), List.of(knifeSeed())))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Knife references unknown maker slug."));
    }

    @Test
    void createMakerSucceeds() throws Exception {
        when(catalogSeedService.createMaker(any())).thenReturn(maker());

        mockMvc.perform(post("/admin/catalog/makers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(makerSeed())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("squid-industries"));
    }

    @Test
    void createMakerRejectsDuplicateSlug() throws Exception {
        when(catalogSeedService.createMaker(any())).thenThrow(new IllegalStateException("Maker slug already exists."));

        mockMvc.perform(post("/admin/catalog/makers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(makerSeed())))
                .andExpect(status().isConflict())
                .andExpect(content().string("Maker slug already exists."));
    }

    @Test
    void updateMakerSucceeds() throws Exception {
        when(catalogSeedService.updateMaker(eq("squid-industries"), any())).thenReturn(maker());

        mockMvc.perform(put("/admin/catalog/makers/{slug}", "squid-industries")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(makerSeed())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("squid-industries"));
    }

    @Test
    void updateMakerReturnsNotFoundForUnknownSlug() throws Exception {
        when(catalogSeedService.updateMaker(eq("bogus"), any()))
                .thenThrow(new IllegalArgumentException("Maker not found."));

        mockMvc.perform(put("/admin/catalog/makers/{slug}", "bogus")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(makerSeed())))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Maker not found."));
    }

    @Test
    void deleteMakerSucceeds() throws Exception {
        mockMvc.perform(delete("/admin/catalog/makers/{slug}", "squid-industries"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMakerRejectsWhenMakerHasKnives() throws Exception {
        doThrow(new IllegalStateException("Cannot delete a maker with knives.")).when(catalogSeedService).deleteMaker("squid-industries");

        mockMvc.perform(delete("/admin/catalog/makers/{slug}", "squid-industries"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Cannot delete a maker with knives."));
    }

    @Test
    void createKnifeSucceeds() throws Exception {
        KnifeDetailDto detail = new KnifeDetailDto("mako", "Mako", "Squid Industries", "squid-industries",
                "Reverse Tanto", "$150-$200", null, "A great trainer.", List.of());
        when(knifeCatalogService.getKnifeBySlug("mako")).thenReturn(detail);

        mockMvc.perform(post("/admin/catalog/knives")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(knifeSeed())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("mako"));
    }

    @Test
    void createKnifeRejectsInvalidData() throws Exception {
        doThrow(new CatalogValidationException("Knife references unknown maker slug."))
                .when(catalogSeedService).createKnife(any());

        mockMvc.perform(post("/admin/catalog/knives")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(knifeSeed())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Knife references unknown maker slug."));
    }

    @Test
    void updateKnifeSucceeds() throws Exception {
        KnifeDetailDto detail = new KnifeDetailDto("mako", "Mako", "Squid Industries", "squid-industries",
                "Reverse Tanto", "$150-$200", null, "Updated description.", List.of());
        when(knifeCatalogService.getKnifeBySlug("mako")).thenReturn(detail);

        mockMvc.perform(put("/admin/catalog/knives/{slug}", "mako")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(knifeSeed())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description."));
    }

    @Test
    void updateKnifeReturnsNotFoundForUnknownSlug() throws Exception {
        doThrow(new IllegalArgumentException("Knife not found.")).when(catalogSeedService).updateKnife(eq("bogus"), any());

        mockMvc.perform(put("/admin/catalog/knives/{slug}", "bogus")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(knifeSeed())))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Knife not found."));
    }

    @Test
    void deleteKnifeSucceeds() throws Exception {
        mockMvc.perform(delete("/admin/catalog/knives/{slug}", "mako"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteKnifeReturnsNotFoundForUnknownSlug() throws Exception {
        doThrow(new IllegalArgumentException("Knife not found.")).when(catalogSeedService).deleteKnife("bogus");

        mockMvc.perform(delete("/admin/catalog/knives/{slug}", "bogus"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Knife not found."));
    }
}
