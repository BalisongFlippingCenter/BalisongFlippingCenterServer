package com.example.BalisongFlipping.implementation;

import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.tokens.EmailVerificationToken;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.EmailTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplementationTest {

    @Mock private JavaMailSender mailSender;
    @Mock private EmailTokenRepository emailTokenRepository;
    @Mock private AccountRepository accountRepository;

    private EmailServiceImplementation emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImplementation();
        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);
        ReflectionTestUtils.setField(emailService, "emailTokenRepository", emailTokenRepository);
        ReflectionTestUtils.setField(emailService, "accountRepository", accountRepository);
    }

    private User user(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    @Test
    void sendEmailBuildsAndSendsMessage() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendEmail("flipper@example.com", "Subject", "Body text");

        verify(mailSender).send(captor.capture());
        assertEquals("flipper@example.com", captor.getValue().getTo()[0]);
        assertEquals("Subject", captor.getValue().getSubject());
        assertEquals("Body text", captor.getValue().getText());
    }

    @Test
    void createNewEmailVerificationTokenSucceeds() throws Exception {
        User u = user(1L, "flipper@example.com");
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(u));
        when(emailTokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        EmailVerificationToken token = emailService.createNewEmailVerificationToken("flipper@example.com");

        assertEquals(u, token.getOwner());
        assertTrue(token.getExpiration().isAfter(Instant.now()));
    }

    @Test
    void createNewEmailVerificationTokenRejectsUnknownEmail() {
        when(accountRepository.findAccountByEmail("nobody@example.com")).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> emailService.createNewEmailVerificationToken("nobody@example.com"));
    }

    @Test
    void createReplacementEmailVerificationTokenDeletesOldTokenFirst() throws Exception {
        User u = user(1L, "flipper@example.com");
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(u));
        when(emailTokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        EmailVerificationToken token = emailService.createReplacementEmailVerificationToken("flipper@example.com");

        verify(emailTokenRepository).deleteByOwner_Id(1L);
        assertEquals(u, token.getOwner());
    }

    @Test
    void createReplacementEmailVerificationTokenRejectsUnknownEmail() {
        when(accountRepository.findAccountByEmail("nobody@example.com")).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> emailService.createReplacementEmailVerificationToken("nobody@example.com"));
    }

    @Test
    void validateEmailTokenVerificationSucceedsAndMarksAccountVerified() throws Exception {
        User u = user(1L, "flipper@example.com");
        u.setEmailVerified(false);
        EmailVerificationToken token = new EmailVerificationToken(u);
        when(emailTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        Boolean result = emailService.validateEmailTokenVerification(token.getToken());

        assertTrue(result);
        assertTrue(u.getEmailVerified());
        verify(accountRepository).save(u);
    }

    @Test
    void validateEmailTokenVerificationReturnsFalseForExpiredToken() throws Exception {
        User u = user(1L, "flipper@example.com");
        EmailVerificationToken token = new EmailVerificationToken(u);
        token.setExpiration(Instant.now().minusSeconds(60));
        when(emailTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        Boolean result = emailService.validateEmailTokenVerification(token.getToken());

        assertFalse(result);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void validateEmailTokenVerificationThrowsWhenTokenNotFound() {
        when(emailTokenRepository.findByToken("bogus")).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> emailService.validateEmailTokenVerification("bogus"));
        assertEquals("Token Not Found", ex.getMessage());
    }
}
