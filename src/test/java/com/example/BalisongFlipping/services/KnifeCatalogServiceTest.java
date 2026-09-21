package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.catalogDtos.KnifeDetailDto;
import com.example.BalisongFlipping.dtos.catalogDtos.KnifeSummaryDto;
import com.example.BalisongFlipping.dtos.catalogDtos.MakerDetailDto;
import com.example.BalisongFlipping.dtos.catalogDtos.MakerSummaryDto;
import com.example.BalisongFlipping.enums.knives.BladeMaterial;
import com.example.BalisongFlipping.enums.knives.BladeStyle;
import com.example.BalisongFlipping.enums.knives.HandleMaterial;
import com.example.BalisongFlipping.enums.knives.PivotSystem;
import com.example.BalisongFlipping.modals.knifeCatalog.Knife;
import com.example.BalisongFlipping.modals.knifeCatalog.KnifeVariant;
import com.example.BalisongFlipping.modals.knifeCatalog.KnifeVersion;
import com.example.BalisongFlipping.modals.knifeCatalog.Maker;
import com.example.BalisongFlipping.repositories.KnifeRepository;
import com.example.BalisongFlipping.repositories.MakerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnifeCatalogServiceTest {

    @Mock private KnifeRepository knifeRepository;
    @Mock private MakerRepository makerRepository;

    private KnifeCatalogService knifeCatalogService;

    @BeforeEach
    void setUp() {
        knifeCatalogService = new KnifeCatalogService(knifeRepository, makerRepository);
    }

    private Maker maker(String slug, String name) {
        Maker m = new Maker();
        m.setSlug(slug);
        m.setName(name);
        return m;
    }

    private KnifeVariant variant(BladeStyle bladeStyle, BladeMaterial bladeMaterial, Double msrp) {
        KnifeVariant v = new KnifeVariant();
        v.setVariantSlug("live");
        v.setBladeStyle(bladeStyle);
        v.setBladeMaterial(bladeMaterial);
        v.setMsrp(msrp);
        return v;
    }

    private KnifeVersion version(HandleMaterial handleMaterial, PivotSystem pivotSystem, Integer releaseYear,
                                  boolean discontinued, List<KnifeVariant> variants) {
        KnifeVersion v = new KnifeVersion();
        v.setVersionSlug("v-" + releaseYear);
        v.setHandleMaterial(handleMaterial);
        v.setPivotSystem(pivotSystem);
        v.setReleaseYear(releaseYear);
        v.setDiscontinued(discontinued);
        v.setVariants(variants);
        v.setWhereToFind(List.of());
        return v;
    }

    private Knife knife(String slug, String name, Maker maker, List<KnifeVersion> versions) {
        Knife k = new Knife();
        k.setSlug(slug);
        k.setName(name);
        k.setMaker(maker);
        k.setVersions(versions);
        return k;
    }

    // -------------------------------------------------------------------------
    // listMakers
    // -------------------------------------------------------------------------

    @Test
    void listMakersReturnsSortedCaseInsensitive() {
        when(makerRepository.findAll()).thenReturn(List.of(
                maker("zebra", "zebra Knives"), maker("acme", "Acme Blades")));

        List<MakerSummaryDto> result = knifeCatalogService.listMakers();

        assertEquals("Acme Blades", result.get(0).name());
        assertEquals("zebra Knives", result.get(1).name());
    }

    // -------------------------------------------------------------------------
    // searchKnives
    // -------------------------------------------------------------------------

    @Test
    void searchKnivesWithBlankSearchReturnsAll() {
        Knife k = knife("mako", "Mako", maker("squid", "Squid"), List.of(version(null, null, 2020, false, List.of())));
        when(knifeRepository.findAll()).thenReturn(List.of(k));

        List<KnifeSummaryDto> result = knifeCatalogService.searchKnives(null, null, null, null, null);

        assertEquals(1, result.size());
    }

    @Test
    void searchKnivesFiltersByBladeMaterial() {
        Knife matching = knife("mako", "Mako", maker("squid", "Squid"),
                List.of(version(null, null, 2020, false, List.of(variant(BladeStyle.DROP_POINT, BladeMaterial.M390, 200.0)))));
        Knife nonMatching = knife("squiddy", "Squiddy", maker("squid", "Squid"),
                List.of(version(null, null, 2020, false, List.of(variant(BladeStyle.TANTO, BladeMaterial.S35VN, 150.0)))));
        when(knifeRepository.findByNameContainingIgnoreCaseOrMakerNameContainingIgnoreCase("mako", "mako"))
                .thenReturn(List.of(matching, nonMatching));

        List<KnifeSummaryDto> result = knifeCatalogService.searchKnives("mako", "M390", null, null, null);

        assertEquals(1, result.size());
        assertEquals("mako", result.get(0).slug());
    }

    @Test
    void searchKnivesFiltersByHandleMaterial() {
        Knife matching = knife("mako", "Mako", maker("squid", "Squid"),
                List.of(version(HandleMaterial.G_10, null, 2020, false, List.of())));
        Knife nonMatching = knife("squiddy", "Squiddy", maker("squid", "Squid"),
                List.of(version(HandleMaterial.CARBON_FIBER, null, 2020, false, List.of())));
        when(knifeRepository.findAll()).thenReturn(List.of(matching, nonMatching));

        List<KnifeSummaryDto> result = knifeCatalogService.searchKnives(null, null, "G_10", null, null);

        assertEquals(1, result.size());
        assertEquals("mako", result.get(0).slug());
    }

    @Test
    void searchKnivesFiltersByPivotSystem() {
        Knife matching = knife("mako", "Mako", maker("squid", "Squid"),
                List.of(version(null, PivotSystem.BUSHINGS, 2020, false, List.of())));
        Knife nonMatching = knife("squiddy", "Squiddy", maker("squid", "Squid"),
                List.of(version(null, PivotSystem.BEARINGS, 2020, false, List.of())));
        when(knifeRepository.findAll()).thenReturn(List.of(matching, nonMatching));

        List<KnifeSummaryDto> result = knifeCatalogService.searchKnives(null, null, null, "bushings", null);

        assertEquals(1, result.size());
    }

    @Test
    void searchKnivesFiltersByMaxPrice() {
        Knife cheap = knife("mako", "Mako", maker("squid", "Squid"),
                List.of(version(null, null, 2020, false, List.of(variant(null, null, 100.0)))));
        Knife expensive = knife("squiddy", "Squiddy", maker("squid", "Squid"),
                List.of(version(null, null, 2020, false, List.of(variant(null, null, 500.0)))));
        when(knifeRepository.findAll()).thenReturn(List.of(cheap, expensive));

        List<KnifeSummaryDto> result = knifeCatalogService.searchKnives(null, null, null, null, 150.0);

        assertEquals(1, result.size());
        assertEquals("mako", result.get(0).slug());
    }

    // -------------------------------------------------------------------------
    // getKnifeBySlug / toDetail
    // -------------------------------------------------------------------------

    @Test
    void getKnifeBySlugReturnsDetailWithVersionsSortedByReleaseYearDescending() {
        Knife k = knife("mako", "Mako", maker("squid", "Squid"), List.of(
                version(null, null, 2018, false, List.of()),
                version(null, null, 2022, false, List.of()),
                version(null, null, null, false, List.of())
        ));
        when(knifeRepository.findBySlug("mako")).thenReturn(Optional.of(k));

        KnifeDetailDto detail = knifeCatalogService.getKnifeBySlug("mako");

        assertEquals(3, detail.versions().size());
        assertEquals(2022, detail.versions().get(0).releaseYear());
        assertEquals(2018, detail.versions().get(1).releaseYear());
        assertEquals(null, detail.versions().get(2).releaseYear());
    }

    @Test
    void getKnifeBySlugThrowsForUnknownSlug() {
        when(knifeRepository.findBySlug("bogus")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> knifeCatalogService.getKnifeBySlug("bogus"));
    }

    // -------------------------------------------------------------------------
    // getMakerBySlug
    // -------------------------------------------------------------------------

    @Test
    void getMakerBySlugReturnsDetailWithKnives() {
        Maker m = maker("squid", "Squid Industries");
        Knife k = knife("mako", "Mako", m, List.of(version(null, null, 2020, false, List.of())));
        when(makerRepository.findBySlug("squid")).thenReturn(Optional.of(m));
        when(knifeRepository.findByMaker(m)).thenReturn(List.of(k));

        MakerDetailDto detail = knifeCatalogService.getMakerBySlug("squid");

        assertEquals("Squid Industries", detail.name());
        assertEquals(1, detail.knives().size());
    }

    @Test
    void getMakerBySlugThrowsForUnknownSlug() {
        when(makerRepository.findBySlug("bogus")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> knifeCatalogService.getMakerBySlug("bogus"));
    }

    // -------------------------------------------------------------------------
    // Summary formatting (bladeStyleSummary / handleMaterialSummary / priceRangeSummary / hasActiveVersion)
    // -------------------------------------------------------------------------

    @Test
    void toSummaryHumanizesAndJoinsDistinctBladeStyles() {
        Knife k = knife("mako", "Mako", maker("squid", "Squid"), List.of(
                version(null, null, 2020, false, List.of(
                        variant(BladeStyle.DROP_POINT, null, null),
                        variant(BladeStyle.DROP_POINT, null, null),
                        variant(BladeStyle.TANTO, null, null)))));
        when(knifeRepository.findBySlug("mako")).thenReturn(Optional.of(k));

        KnifeDetailDto detail = knifeCatalogService.getKnifeBySlug("mako");

        assertEquals("Drop Point / Tanto", detail.bladeStyleSummary());
    }

    @Test
    void toSummaryFormatsPriceRangeAsSingleValueWhenAllEqual() {
        Knife k = knife("mako", "Mako", maker("squid", "Squid"), List.of(
                version(null, null, 2020, false, List.of(variant(null, null, 150.0), variant(null, null, 150.0)))));
        when(knifeRepository.findBySlug("mako")).thenReturn(Optional.of(k));

        assertEquals("$150", knifeCatalogService.getKnifeBySlug("mako").priceRangeSummary());
    }

    @Test
    void toSummaryFormatsPriceRangeAsSpanWhenPricesDiffer() {
        Knife k = knife("mako", "Mako", maker("squid", "Squid"), List.of(
                version(null, null, 2020, false, List.of(variant(null, null, 150.0), variant(null, null, 200.0)))));
        when(knifeRepository.findBySlug("mako")).thenReturn(Optional.of(k));

        assertEquals("$150–$200", knifeCatalogService.getKnifeBySlug("mako").priceRangeSummary());
    }

    @Test
    void toSummaryReturnsNullPriceRangeWhenNoVariants() {
        Knife k = knife("mako", "Mako", maker("squid", "Squid"), List.of(version(null, null, 2020, false, List.of())));
        when(knifeRepository.findBySlug("mako")).thenReturn(Optional.of(k));

        assertEquals(null, knifeCatalogService.getKnifeBySlug("mako").priceRangeSummary());
    }

    @Test
    void searchKnivesMarksHasActiveVersionFalseWhenAllDiscontinued() {
        Knife k = knife("mako", "Mako", maker("squid", "Squid"), List.of(version(null, null, 2020, true, List.of())));
        when(knifeRepository.findAll()).thenReturn(List.of(k));

        assertFalse(knifeCatalogService.searchKnives(null, null, null, null, null).get(0).hasActiveVersion());
    }

    @Test
    void searchKnivesMarksHasActiveVersionTrueWhenAnyVersionActive() {
        Knife k = knife("mako", "Mako", maker("squid", "Squid"), List.of(
                version(null, null, 2018, true, List.of()), version(null, null, 2022, false, List.of())));
        when(knifeRepository.findAll()).thenReturn(List.of(k));

        assertTrue(knifeCatalogService.searchKnives(null, null, null, null, null).get(0).hasActiveVersion());
    }
}
