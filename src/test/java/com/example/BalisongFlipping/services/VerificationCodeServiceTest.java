package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.tokens.EmailVerificationToken;
import com.example.BalisongFlipping.repositories.EmailTokenRepository;
import com.example.BalisongFlipping.utils.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceTest {

    @Mock private EmailTokenRepository emailTokenRepository;

    private VerificationCodeService verificationCodeService;
    private User user;

    @BeforeEach
    void setUp() {
        verificationCodeService = new VerificationCodeService(emailTokenRepository);
        user = new User();
        user.setId(1L);
    }

    private EmailVerificationToken tokenWithCode(String code) {
        EmailVerificationToken token = new EmailVerificationToken(user);
        token.setToken(code);
        when(emailTokenRepository.findFirstByOwner_IdOrderByIdDesc(1L)).thenReturn(Optional.of(token));
        return token;
    }

    @Test
    void issueReplacesExistingCodesAndReturnsSixDigits() {
        when(emailTokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        String code = verificationCodeService.issue(user);

        verify(emailTokenRepository).deleteByOwner_Id(1L);
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    void issueBlocksAFourthCodeWithinTheWindow() {
        when(emailTokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));
        for (int i = 0; i < 3; i++) verificationCodeService.issue(user);

        assertThrows(TooManyRequestsException.class, () -> verificationCodeService.issue(user));
    }

    @Test
    void verifyConsumesTheCodeOnSuccess() throws Exception {
        EmailVerificationToken token = tokenWithCode("123456");

        verificationCodeService.verify(user, "123456");

        verify(emailTokenRepository).delete(token);
    }

    @Test
    void verifyCountsAWrongCode() {
        EmailVerificationToken token = tokenWithCode("123456");

        Exception ex = assertThrows(Exception.class, () -> verificationCodeService.verify(user, "000000"));

        assertEquals("Invalid code.", ex.getMessage());
        assertEquals(1, token.getFailedAttempts());
        verify(emailTokenRepository).save(token);
        verify(emailTokenRepository, never()).delete(any());
    }

    @Test
    void verifyDeletesTheCodeOnTheFifthWrongAttempt() {
        EmailVerificationToken token = tokenWithCode("123456");
        token.setFailedAttempts(4);

        Exception ex = assertThrows(Exception.class, () -> verificationCodeService.verify(user, "000000"));

        assertEquals("Too many incorrect attempts. Please request a new code.", ex.getMessage());
        verify(emailTokenRepository).delete(token);
    }

    @Test
    void verifyRejectsNullCode() {
        tokenWithCode("123456");

        Exception ex = assertThrows(Exception.class, () -> verificationCodeService.verify(user, null));
        assertEquals("Invalid code.", ex.getMessage());
    }

    @Test
    void verifyRejectsExpiredCode() {
        EmailVerificationToken token = tokenWithCode("123456");
        token.setExpiration(Instant.now().minusSeconds(60));

        Exception ex = assertThrows(Exception.class, () -> verificationCodeService.verify(user, "123456"));
        assertEquals("Code has expired. Please request a new one.", ex.getMessage());
    }

    @Test
    void verifyRejectsWhenNoCodeWasIssued() {
        when(emailTokenRepository.findFirstByOwner_IdOrderByIdDesc(1L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> verificationCodeService.verify(user, "123456"));
        assertEquals("Invalid code.", ex.getMessage());
    }
}
