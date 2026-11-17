package com.example.BalisongFlipping;

import com.example.BalisongFlipping.dtos.ConfirmForgotPasswordDto;
import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.tokens.EmailVerificationToken;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.EmailTokenRepository;
import com.example.BalisongFlipping.services.AuthService;
import com.example.BalisongFlipping.services.EmailService;
import com.example.BalisongFlipping.services.VerificationCodeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class VerificationCodeIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private AuthService authService;
    @Autowired private VerificationCodeService verificationCodeService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private EmailTokenRepository emailTokenRepository;

    @MockBean private EmailService emailService;

    @AfterEach
    void cleanUp() {
        emailTokenRepository.deleteAll();
        accountRepository.deleteAll();
    }

    private Account saveAccount(String email) {
        User user = new User(email, "hashed-password", "flipper", "1234");
        user.setEmailVerified(false);
        return accountRepository.save(user);
    }

    private String wrongCode(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }

    @Test
    void forgotPasswordIssuesAndEmailsACode() throws Exception {
        Account account = saveAccount("forgot@example.com");

        authService.forgotPassword("forgot@example.com");

        assertTrue(emailTokenRepository.findFirstByOwner_IdOrderByIdDesc(account.getId()).isPresent());
        verify(emailService).sendEmail(eq("forgot@example.com"), anyString(), anyString());
    }

    @Test
    void failedAttemptIsKeptWhenTheCallersTransactionThrows() {
        Account account = saveAccount("attempts@example.com");
        String code = verificationCodeService.issue(account);

        assertThrows(Exception.class, () -> authService.confirmForgotPassword(
                new ConfirmForgotPasswordDto("attempts@example.com", wrongCode(code), "new-password")));

        EmailVerificationToken token = emailTokenRepository.findFirstByOwner_IdOrderByIdDesc(account.getId()).orElseThrow();
        assertEquals(1, token.getFailedAttempts());
    }

    @Test
    void fifthWrongAttemptBurnsTheCode() {
        Account account = saveAccount("burn@example.com");
        String code = verificationCodeService.issue(account);

        for (int i = 0; i < 5; i++) {
            assertThrows(Exception.class, () -> authService.confirmForgotPassword(
                    new ConfirmForgotPasswordDto("burn@example.com", wrongCode(code), "new-password")));
        }

        assertTrue(emailTokenRepository.findFirstByOwner_IdOrderByIdDesc(account.getId()).isEmpty());
        assertThrows(Exception.class, () -> authService.confirmForgotPassword(
                new ConfirmForgotPasswordDto("burn@example.com", code, "new-password")));
    }

    @Test
    void anotherAccountsCodeDoesNotVerifyThisAccount() throws Exception {
        Account victim = saveAccount("victim@example.com");
        Account attacker = saveAccount("attacker@example.com");
        verificationCodeService.issue(victim);
        String attackerCode = verificationCodeService.issue(attacker);

        assertThrows(Exception.class, () -> authService.validateEmailVerification("victim@example.com", attackerCode));

        assertFalse(accountRepository.findById(victim.getId()).orElseThrow().getEmailVerified());
    }

    @Test
    void correctCodeVerifiesTheEmail() throws Exception {
        Account account = saveAccount("verify@example.com");
        String code = verificationCodeService.issue(account);

        assertTrue(authService.validateEmailVerification("verify@example.com", code));

        assertTrue(accountRepository.findById(account.getId()).orElseThrow().getEmailVerified());
        assertTrue(emailTokenRepository.findFirstByOwner_IdOrderByIdDesc(account.getId()).isEmpty());
    }
}
