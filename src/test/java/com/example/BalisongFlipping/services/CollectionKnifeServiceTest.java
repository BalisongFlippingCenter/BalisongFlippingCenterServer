package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.modals.collectionKnives.CollectionKnife;
import com.example.BalisongFlipping.repositories.CollectionKnifeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionKnifeServiceTest {

    @Mock private CollectionKnifeRepository collectionKnifeRepository;

    private CollectionKnifeService collectionKnifeService;

    @BeforeEach
    void setUp() {
        collectionKnifeService = new CollectionKnifeService();
        ReflectionTestUtils.setField(collectionKnifeService, "collectionKnifeRepository", collectionKnifeRepository);
    }

    private CollectionKnife knife(String displayName, String coverPhoto) {
        CollectionKnife k = new CollectionKnife();
        k.setDisplayName(displayName);
        k.setCoverPhoto(coverPhoto);
        return k;
    }

    @Test
    void getCollectionKnifeCoverPhotoReturnsPhoto() throws Exception {
        when(collectionKnifeRepository.findById(7L)).thenReturn(Optional.of(knife("Mako", "https://cdn/cover.png")));
        assertEquals("https://cdn/cover.png", collectionKnifeService.getCollectionKnifeCoverPhoto("7"));
    }

    @Test
    void getCollectionKnifeCoverPhotoThrowsForUnknownKnife() {
        when(collectionKnifeRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> collectionKnifeService.getCollectionKnifeCoverPhoto("999"));
    }

    @Test
    void getCollectionKnifeDisplayNameReturnsName() throws Exception {
        when(collectionKnifeRepository.findById(7L)).thenReturn(Optional.of(knife("Mako", "https://cdn/cover.png")));
        assertEquals("Mako", collectionKnifeService.getCollectionKnifeDisplayName("7"));
    }

    @Test
    void getCollectionKnifeDisplayNameThrowsForUnknownKnife() {
        when(collectionKnifeRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> collectionKnifeService.getCollectionKnifeDisplayName("999"));
    }
}
