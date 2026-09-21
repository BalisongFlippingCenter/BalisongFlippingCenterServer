package com.example.BalisongFlipping.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaFSServiceTest {

    private final JavaFSService javaFSService = new JavaFSService();

    @Test
    void addAssetIsNotYetImplemented() {
        assertThrows(UnsupportedOperationException.class, () -> javaFSService.addAsset("title", null));
    }

    @Test
    void getAssetIsNotYetImplemented() {
        assertThrows(UnsupportedOperationException.class, () -> javaFSService.getAsset("id"));
    }

    @Test
    void deleteAssetIsNotYetImplemented() {
        assertThrows(UnsupportedOperationException.class, () -> javaFSService.deleteAsset("id"));
    }
}
