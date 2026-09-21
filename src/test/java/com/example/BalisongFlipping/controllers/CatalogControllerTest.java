package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.catalogDtos.KnifeDetailDto;
import com.example.BalisongFlipping.dtos.catalogDtos.KnifeSummaryDto;
import com.example.BalisongFlipping.dtos.catalogDtos.MakerDetailDto;
import com.example.BalisongFlipping.dtos.catalogDtos.MakerSummaryDto;
import com.example.BalisongFlipping.services.JwtService;
import com.example.BalisongFlipping.services.KnifeCatalogService;
import com.example.BalisongFlipping.services.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnifeCatalogService knifeCatalogService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    private KnifeSummaryDto knifeSummary() {
        return new KnifeSummaryDto("mako", "Mako", "Squid Industries", "squid-industries",
                "Reverse Tanto", "G10", "$150-$200", null, true);
    }

    @Test
    void searchKnivesReturnsResults() throws Exception {
        when(knifeCatalogService.searchKnives(any(), any(), any(), any(), any()))
                .thenReturn(List.of(knifeSummary()));

        mockMvc.perform(get("/catalog/any/knives").param("search", "mako"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("mako"));
    }

    @Test
    void searchKnivesRejectsServiceFailure() throws Exception {
        when(knifeCatalogService.searchKnives(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Search failed"));

        mockMvc.perform(get("/catalog/any/knives"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Search failed"));
    }

    @Test
    void getKnifeReturnsDetail() throws Exception {
        KnifeDetailDto detail = new KnifeDetailDto("mako", "Mako", "Squid Industries", "squid-industries",
                "Reverse Tanto", "$150-$200", null, "A great trainer.", List.of());
        when(knifeCatalogService.getKnifeBySlug("mako")).thenReturn(detail);

        mockMvc.perform(get("/catalog/any/knives/{slug}", "mako"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mako"));
    }

    @Test
    void getKnifeReturnsNotFoundForUnknownSlug() throws Exception {
        when(knifeCatalogService.getKnifeBySlug("bogus"))
                .thenThrow(new IllegalArgumentException("Knife not found."));

        mockMvc.perform(get("/catalog/any/knives/{slug}", "bogus"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Knife not found."));
    }

    @Test
    void listMakersReturnsResults() throws Exception {
        when(knifeCatalogService.listMakers())
                .thenReturn(List.of(new MakerSummaryDto("squid-industries", "Squid Industries", "USA", null)));

        mockMvc.perform(get("/catalog/any/makers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Squid Industries"));
    }

    @Test
    void getMakerReturnsDetail() throws Exception {
        MakerDetailDto detail = new MakerDetailDto("squid-industries", "Squid Industries", "USA",
                "Trainers", null, null, 2014, null, null, null, null, List.of(knifeSummary()));
        when(knifeCatalogService.getMakerBySlug("squid-industries")).thenReturn(detail);

        mockMvc.perform(get("/catalog/any/makers/{slug}", "squid-industries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Squid Industries"))
                .andExpect(jsonPath("$.knives[0].slug").value("mako"));
    }

    @Test
    void getMakerReturnsNotFoundForUnknownSlug() throws Exception {
        when(knifeCatalogService.getMakerBySlug("bogus"))
                .thenThrow(new IllegalArgumentException("Maker not found."));

        mockMvc.perform(get("/catalog/any/makers/{slug}", "bogus"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Maker not found."));
    }
}
