package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.CollectionDataDto;
import com.example.BalisongFlipping.dtos.UpdateKnifeDto;
import com.example.BalisongFlipping.modals.collectionKnives.CollectionKnife;
import com.example.BalisongFlipping.modals.collections.Collection;
import com.example.BalisongFlipping.repositories.CollectionKnifeRepository;
import com.example.BalisongFlipping.repositories.CollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    @Mock private CollectionRepository collectionRepository;
    @Mock private CollectionKnifeRepository collectionKnifeRepository;
    @Mock private PostService postService;

    private CollectionService collectionService;

    @BeforeEach
    void setUp() {
        collectionService = new CollectionService(collectionRepository, collectionKnifeRepository);
        ReflectionTestUtils.setField(collectionService, "postService", postService);
    }

    private Collection collection(Long id, Long userId) {
        Collection c = new Collection(userId);
        c.setId(id);
        return c;
    }

    private CollectionKnife knife(Long id, Long collectionId, String displayName) {
        CollectionKnife k = new CollectionKnife();
        k.setId(id);
        k.setCollectionId(collectionId);
        k.setDisplayName(displayName);
        return k;
    }

    // -------------------------------------------------------------------------
    // Collection lookups
    // -------------------------------------------------------------------------

    @Test
    void getCollectionReturnsDto() {
        Collection c = collection(5L, 1L);
        when(collectionRepository.findById(5L)).thenReturn(Optional.of(c));
        when(collectionKnifeRepository.findAllByCollectionId(5L)).thenReturn(Optional.of(List.of()));

        CollectionDataDto dto = collectionService.getCollection("5");

        assertEquals("5", dto.id());
        assertEquals("1", dto.accountId());
    }

    @Test
    void getCollectionReturnsNullForUnknownId() {
        when(collectionRepository.findById(999L)).thenReturn(Optional.empty());
        assertNull(collectionService.getCollection("999"));
    }

    @Test
    void getCollectionByAccountIdReturnsDto() {
        Collection c = collection(5L, 1L);
        when(collectionRepository.findByUserId(1L)).thenReturn(Optional.of(c));
        when(collectionKnifeRepository.findAllByCollectionId(5L)).thenReturn(Optional.of(List.of()));

        assertEquals("5", collectionService.getCollectionByAccountId("1").id());
    }

    @Test
    void checkForCollectionExistanceReturnsTrueWhenPresent() {
        when(collectionRepository.findById(5L)).thenReturn(Optional.of(collection(5L, 1L)));
        assertTrue(collectionService.checkForCollectionExistance("5"));
    }

    @Test
    void checkForCollectionExistanceReturnsFalseWhenAbsent() {
        when(collectionRepository.findById(999L)).thenReturn(Optional.empty());
        assertFalse(collectionService.checkForCollectionExistance("999"));
    }

    // -------------------------------------------------------------------------
    // Banner
    // -------------------------------------------------------------------------

    @Test
    void updateBannerImgSucceeds() {
        Collection c = collection(5L, 1L);
        when(collectionRepository.findById(5L)).thenReturn(Optional.of(c));
        when(collectionRepository.save(c)).thenReturn(c);
        when(collectionKnifeRepository.findAllByCollectionId(5L)).thenReturn(Optional.of(List.of()));

        CollectionDataDto dto = collectionService.updateBannerImg("5", "https://cdn/banner.png");

        assertEquals("https://cdn/banner.png", c.getBannerImg());
        assertEquals("https://cdn/banner.png", dto.bannerImage());
    }

    @Test
    void updateBannerImgReturnsNullForUnknownCollection() {
        when(collectionRepository.findById(999L)).thenReturn(Optional.empty());
        assertNull(collectionService.updateBannerImg("999", "https://cdn/banner.png"));
    }

    // -------------------------------------------------------------------------
    // Featured knife
    // -------------------------------------------------------------------------

    @Test
    void setFeaturedKnifeSucceeds() throws Exception {
        Collection c = collection(5L, 1L);
        when(collectionRepository.findById(5L)).thenReturn(Optional.of(c));
        when(collectionKnifeRepository.findAllByCollectionId(5L)).thenReturn(Optional.of(List.of(knife(7L, 5L, "Mako"))));
        when(collectionRepository.save(c)).thenReturn(c);

        collectionService.setFeaturedKnife("5", "7");

        assertEquals(7L, c.getFeaturedKnife());
    }

    @Test
    void setFeaturedKnifeRejectsKnifeNotInCollection() {
        Collection c = collection(5L, 1L);
        when(collectionRepository.findById(5L)).thenReturn(Optional.of(c));
        when(collectionKnifeRepository.findAllByCollectionId(5L)).thenReturn(Optional.of(List.of()));

        Exception ex = assertThrows(Exception.class, () -> collectionService.setFeaturedKnife("5", "7"));
        assertEquals("Knife does not belong to this collection.", ex.getMessage());
    }

    @Test
    void setFeaturedKnifeRejectsUnknownCollection() {
        when(collectionRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> collectionService.setFeaturedKnife("999", "7"));
    }

    @Test
    void clearFeaturedKnifeSucceeds() throws Exception {
        Collection c = collection(5L, 1L);
        c.setFeaturedKnife(7L);
        when(collectionRepository.findById(5L)).thenReturn(Optional.of(c));
        when(collectionRepository.save(c)).thenReturn(c);
        when(collectionKnifeRepository.findAllByCollectionId(5L)).thenReturn(Optional.of(List.of()));

        collectionService.clearFeaturedKnife("5");

        assertNull(c.getFeaturedKnife());
    }

    // -------------------------------------------------------------------------
    // Knife validation
    // -------------------------------------------------------------------------

    @Test
    void validateNewKnifeInfoAcceptsCompleteInfo() {
        MultipartFile cover = new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1});
        assertTrue(collectionService.validateNewKnifeInfo("Mako", "Squid Industries", "Mako", "trainer", "2024-01-01", cover));
    }

    @Test
    void validateNewKnifeInfoRejectsEmptyCoverPhoto() {
        MultipartFile emptyCover = new MockMultipartFile("file", "cover.png", "image/png", new byte[0]);
        assertFalse(collectionService.validateNewKnifeInfo("Mako", "Squid Industries", "Mako", "trainer", "2024-01-01", emptyCover));
    }

    @Test
    void validateNewKnifeInfoRejectsEmptyDisplayName() {
        MultipartFile cover = new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1});
        assertFalse(collectionService.validateNewKnifeInfo("", "Squid Industries", "Mako", "trainer", "2024-01-01", cover));
    }

    @Test
    void checkForDuplicateDisplayNameFindsMatch() throws Exception {
        when(collectionKnifeRepository.findAllByCollectionId(5L)).thenReturn(Optional.of(List.of(knife(7L, 5L, "Mako"))));
        assertTrue(collectionService.checkForDuplicateDisplayName("Mako", "5"));
    }

    @Test
    void checkForDuplicateDisplayNameReturnsFalseWhenNoMatch() throws Exception {
        when(collectionKnifeRepository.findAllByCollectionId(5L)).thenReturn(Optional.of(List.of(knife(7L, 5L, "Mako"))));
        assertFalse(collectionService.checkForDuplicateDisplayName("Squiddy", "5"));
    }

    @Test
    void checkForDuplicateDisplayNameExcludesGivenKnifeId() throws Exception {
        when(collectionKnifeRepository.findAllByCollectionId(5L)).thenReturn(Optional.of(List.of(knife(7L, 5L, "Mako"))));
        assertFalse(collectionService.checkForDuplicateDisplayName("Mako", "5", 7L));
    }

    // -------------------------------------------------------------------------
    // Knife lookups
    // -------------------------------------------------------------------------

    @Test
    void getKnifeByIdReturnsKnife() {
        when(collectionKnifeRepository.findById(7L)).thenReturn(Optional.of(knife(7L, 5L, "Mako")));
        assertEquals("Mako", collectionService.getKnifeById(7L).getDisplayName());
    }

    @Test
    void getKnifeByIdReturnsNullForUnknownKnife() {
        when(collectionKnifeRepository.findById(999L)).thenReturn(Optional.empty());
        assertNull(collectionService.getKnifeById(999L));
    }

    @Test
    void getKnifeDisplayNameSanitizesName() throws Exception {
        when(collectionKnifeRepository.findById(7L)).thenReturn(Optional.of(knife(7L, 5L, "Mako #1!")));
        assertEquals("Mako__1_", collectionService.getKnifeDisplayName("5", "7"));
    }

    @Test
    void getKnifeDisplayNameRejectsWrongCollection() {
        when(collectionKnifeRepository.findById(7L)).thenReturn(Optional.of(knife(7L, 5L, "Mako")));
        Exception ex = assertThrows(Exception.class, () -> collectionService.getKnifeDisplayName("999", "7"));
        assertEquals("Knife does not belong to this collection.", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // Update knife cover photo
    // -------------------------------------------------------------------------

    @Test
    void updateKnifeCoverPhotoSucceeds() throws Exception {
        CollectionKnife k = knife(7L, 5L, "Mako");
        when(collectionKnifeRepository.findById(7L)).thenReturn(Optional.of(k));
        when(collectionKnifeRepository.save(k)).thenReturn(k);

        collectionService.updateKnifeCoverPhoto("5", "7", "https://cdn/cover.png");

        assertEquals("https://cdn/cover.png", k.getCoverPhoto());
    }

    @Test
    void updateKnifeCoverPhotoRejectsWrongCollection() {
        when(collectionKnifeRepository.findById(7L)).thenReturn(Optional.of(knife(7L, 5L, "Mako")));
        assertThrows(Exception.class, () -> collectionService.updateKnifeCoverPhoto("999", "7", "https://cdn/cover.png"));
    }

    // -------------------------------------------------------------------------
    // Delete knife
    // -------------------------------------------------------------------------

    @Test
    void deleteKnifeSucceeds() throws Exception {
        when(collectionKnifeRepository.findById(7L)).thenReturn(Optional.of(knife(7L, 5L, "Mako")));
        when(collectionRepository.findById(5L)).thenReturn(Optional.of(collection(5L, 1L)));

        collectionService.deleteKnife("5", "7");

        verify(collectionKnifeRepository).deleteById(7L);
    }

    @Test
    void deleteKnifeClearsFeaturedKnifeWhenDeletingIt() throws Exception {
        when(collectionKnifeRepository.findById(7L)).thenReturn(Optional.of(knife(7L, 5L, "Mako")));
        Collection c = collection(5L, 1L);
        c.setFeaturedKnife(7L);
        when(collectionRepository.findById(5L)).thenReturn(Optional.of(c));

        collectionService.deleteKnife("5", "7");

        assertNull(c.getFeaturedKnife());
        verify(collectionRepository).save(c);
    }

    @Test
    void deleteKnifeRejectsWrongCollection() {
        when(collectionKnifeRepository.findById(7L)).thenReturn(Optional.of(knife(7L, 5L, "Mako")));
        assertThrows(Exception.class, () -> collectionService.deleteKnife("999", "7"));
        verify(collectionKnifeRepository, never()).deleteById(org.mockito.ArgumentMatchers.anyLong());
    }

    // -------------------------------------------------------------------------
    // Update knife
    // -------------------------------------------------------------------------

    private UpdateKnifeDto updateDto(String displayName) {
        return new UpdateKnifeDto(displayName, "Squid Industries", "Mako", "trainer",
                "2024-01-01", true, false, 150.0, 5.5, 4.0, "bushings", "latchless", "zen pins",
                false, null, "reverse-tanto", "stonewash", "s35vn", "channel", "g10", "stonewash",
                8, 6, 7, 5, 9);
    }

    @Test
    void updateKnifeSucceedsAndRecalculatesAverageScore() throws Exception {
        CollectionKnife k = knife(7L, 5L, "OldName");
        when(collectionKnifeRepository.findById(7L)).thenReturn(Optional.of(k));
        when(collectionKnifeRepository.findAllByCollectionId(5L)).thenReturn(Optional.of(List.of(k)));
        when(collectionKnifeRepository.save(k)).thenReturn(k);

        CollectionKnife result = collectionService.updateKnife("5", "7", updateDto("NewName"));

        assertEquals("NewName", result.getDisplayName());
        assertEquals(7.0, result.getAverageScore());
    }

    @Test
    void updateKnifeRejectsDuplicateDisplayName() {
        CollectionKnife k = knife(7L, 5L, "OldName");
        CollectionKnife other = knife(8L, 5L, "TakenName");
        when(collectionKnifeRepository.findById(7L)).thenReturn(Optional.of(k));
        when(collectionKnifeRepository.findAllByCollectionId(5L)).thenReturn(Optional.of(List.of(k, other)));

        Exception ex = assertThrows(Exception.class, () -> collectionService.updateKnife("5", "7", updateDto("TakenName")));
        assertEquals("A knife with that display name already exists in your collection.", ex.getMessage());
    }

    @Test
    void updateKnifeRejectsWrongCollection() {
        when(collectionKnifeRepository.findById(7L)).thenReturn(Optional.of(knife(7L, 5L, "OldName")));
        assertThrows(Exception.class, () -> collectionService.updateKnife("999", "7", updateDto("NewName")));
    }

    // -------------------------------------------------------------------------
    // Add knife
    // -------------------------------------------------------------------------

    @Test
    void addNewKnifeConstructsAndSaves() throws Exception {
        when(collectionKnifeRepository.save(org.mockito.ArgumentMatchers.any(CollectionKnife.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CollectionKnife result = collectionService.addNewKnife(
                "5", "Mako", "Squid Industries", "Mako", "trainer", "2024-01-01",
                "true", "false", "https://cdn/cover.png",
                "150", "5.5", "4.0", "bushings", "latchless", "zen pins",
                "false", null, "reverse-tanto", "stonewash", "s35vn",
                "channel", "g10", "stonewash",
                8, 6, 7, 5, 9, List.of("https://cdn/gallery1.png"));

        assertEquals("Mako", result.getDisplayName());
        assertEquals(5L, result.getCollectionId());
        assertEquals(1, result.getGalleryFiles().size());
        assertEquals(7.0, result.getAverageScore());
    }
}
