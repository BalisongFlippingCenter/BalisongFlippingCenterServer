package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.tokens.RefreshToken;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private AccountRepository accountRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService();
        org.springframework.test.util.ReflectionTestUtils.setField(refreshTokenService, "refreshTokenRepository", refreshTokenRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(refreshTokenService, "accountRepository", accountRepository);
    }

    private User user(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    @Test
    void createRefreshTokenDeletesOldTokensAndSavesNew() throws Exception {
        User u = user(1L, "flipper@example.com");
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(u));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken("flipper@example.com");

        verify(refreshTokenRepository).deleteByOwner_Id(1L);
        assertEquals(u, token.getOwner());
    }

    @Test
    void createRefreshTokenRejectsUnknownEmail() {
        when(accountRepository.findAccountByEmail("nobody@example.com")).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> refreshTokenService.createRefreshToken("nobody@example.com"));
        assertEquals("Couldn't find account.", ex.getMessage());
    }

    @Test
    void findByTokenDelegatesToRepository() {
        RefreshToken token = new RefreshToken();
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        assertEquals(token, refreshTokenService.findByToken("abc").get());
    }

    @Test
    void verityExpirationReturnsTokenWhenNotExpired() {
        RefreshToken token = new RefreshToken();
        token.setExpiration(Instant.now().plusSeconds(3600));

        assertEquals(token, refreshTokenService.verityExpiration(token));
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void verityExpirationDeletesAndThrowsWhenExpired() {
        RefreshToken token = new RefreshToken();
        token.setExpiration(Instant.now().minusSeconds(60));

        assertThrows(RuntimeException.class, () -> refreshTokenService.verityExpiration(token));
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void removeRefreshTokenDeletesWhenFound() throws Exception {
        RefreshToken token = new RefreshToken();
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        refreshTokenService.removeRefreshToken("abc");

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void removeRefreshTokenNoOpsWhenNotFound() throws Exception {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        refreshTokenService.removeRefreshToken("missing");

        verify(refreshTokenRepository, never()).delete(any());
    }
}
