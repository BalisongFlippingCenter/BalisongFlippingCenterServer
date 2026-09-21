package com.example.BalisongFlipping.implementation;

import com.example.BalisongFlipping.dtos.ConfirmForgotPasswordDto;
import com.example.BalisongFlipping.dtos.GoogleSignInResult;
import com.example.BalisongFlipping.dtos.LoginAccountDto;
import com.example.BalisongFlipping.dtos.RegisterAccountDto;
import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.collections.Collection;
import com.example.BalisongFlipping.modals.tokens.EmailVerificationToken;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.CollectionRepository;
import com.example.BalisongFlipping.repositories.EmailTokenRepository;
import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplementationTest {

    @Mock private AccountRepository accountRepository;
    @Mock private CollectionRepository collectionRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AccountService accountService;
    @Mock private EmailService emailService;
    @Mock private EmailTokenRepository emailTokenRepository;
    @Mock private RestTemplate restTemplate;

    private AuthServiceImplementation authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImplementation(accountRepository, collectionRepository, authenticationManager, passwordEncoder);
        ReflectionTestUtils.setField(authService, "accountService", accountService);
        ReflectionTestUtils.setField(authService, "emailService", emailService);
        ReflectionTestUtils.setField(authService, "emailTokenRepository", emailTokenRepository);
        ReflectionTestUtils.setField(authService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(authService, "adminBootstrapEmail", "");
    }

    private RegisterAccountDto registerDto(String email, String displayName, String password) {
        return new RegisterAccountDto(email, displayName, password);
    }

    // -------------------------------------------------------------------------
    // validateNewUser
    // -------------------------------------------------------------------------

    @Test
    void validateNewUserAcceptsGoodInfo() {
        assertTrue(authService.validateNewUser(registerDto("flipper@example.com", "Flipper_1", "correct-password")));
    }

    @Test
    void validateNewUserRejectsEmptyPassword() {
        assertFalse(authService.validateNewUser(registerDto("flipper@example.com", "Flipper", "")));
    }

    @Test
    void validateNewUserRejectsEmptyEmail() {
        assertFalse(authService.validateNewUser(registerDto("", "Flipper", "correct-password")));
    }

    @Test
    void validateNewUserRejectsShortPassword() {
        assertFalse(authService.validateNewUser(registerDto("flipper@example.com", "Flipper", "short")));
    }

    @Test
    void validateNewUserRejectsShortDisplayName() {
        assertFalse(authService.validateNewUser(registerDto("flipper@example.com", "Fl", "correct-password")));
    }

    @Test
    void validateNewUserRejectsDisallowedCharacters() {
        assertFalse(authService.validateNewUser(registerDto("flipper@example.com", "Flipper$$$", "correct-password")));
    }

    @Test
    void validateNewUserRejectsProfaneDisplayName() {
        // HARD_BANNED words are flagged as a substring match, unlike WORD_BANNED which requires a whole-token match
        assertFalse(authService.validateNewUser(registerDto("flipper@example.com", "cuntflip", "correct-password")));
    }

    // -------------------------------------------------------------------------
    // signup
    // -------------------------------------------------------------------------

    @Test
    void signupReturnsNullForExistingEmail() throws Exception {
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(new User()));

        assertNull(authService.signup(registerDto("flipper@example.com", "Flipper", "correct-password")));

        verify(accountRepository, never()).save(any());
    }

    @Test
    void signupCreatesUnverifiedUserAndSendsVerificationEmail() throws Exception {
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.empty());
        when(accountService.generateIdentifierCode("Flipper")).thenReturn("0001");
        when(passwordEncoder.encode("correct-password")).thenReturn("hashed");
        when(accountRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(collectionRepository.save(any(Collection.class))).thenAnswer(invocation -> {
            Collection c = invocation.getArgument(0);
            c.setId(5L);
            return c;
        });
        when(emailService.createNewEmailVerificationToken("flipper@example.com"))
                .thenReturn(new EmailVerificationToken(new User()));

        Account saved = authService.signup(registerDto("flipper@example.com", "Flipper", "correct-password"));

        assertEquals("USER", saved.getRole());
        assertFalse(saved.getEmailVerified());
        verify(emailService).sendEmail(eq("flipper@example.com"), eq("Email Verification"), anyString());
        verify(emailService, never()).sendEmail(eq("flipper@example.com"), eq("Balisong Flipping Hub — Admin Login Code"), anyString());
    }

    @Test
    void signupBootstrapsAdminRoleAndSendsLoginCodeInstead() throws Exception {
        ReflectionTestUtils.setField(authService, "adminBootstrapEmail", "admin@example.com");
        when(accountRepository.findAccountByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(accountService.generateIdentifierCode("AdminFlip")).thenReturn("0001");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(accountRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(collectionRepository.save(any(Collection.class))).thenAnswer(invocation -> {
            Collection c = invocation.getArgument(0);
            c.setId(5L);
            return c;
        });

        Account saved = authService.signup(registerDto("admin@example.com", "AdminFlip", "correct-password"));

        assertEquals("ADMIN", saved.getRole());
        verify(emailService).sendEmail(eq("admin@example.com"), eq("Balisong Flipping Hub — Admin Login Code"), anyString());
        verify(emailService, never()).createNewEmailVerificationToken(anyString());
    }

    // -------------------------------------------------------------------------
    // authenticate
    // -------------------------------------------------------------------------

    private User activeUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("flipper@example.com");
        return user;
    }

    @Test
    void authenticateSucceedsForActiveAccount() {
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(activeUser()));

        Account result = authService.authenticate(new LoginAccountDto("flipper@example.com", "correct-password"));

        assertEquals("flipper@example.com", result.getEmail());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void authenticateRejectsBannedAccountBeforePasswordCheck() {
        User banned = activeUser();
        banned.setBanned(true);
        banned.setBanReason("Repeated spam");
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(banned));

        DisabledException ex = assertThrows(DisabledException.class,
                () -> authService.authenticate(new LoginAccountDto("flipper@example.com", "correct-password")));

        assertTrue(ex.getMessage().contains("Repeated spam"));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void authenticateRejectsSuspendedAccountBeforePasswordCheck() {
        User suspended = activeUser();
        suspended.setSuspendedUntil(Instant.now().plusSeconds(3600));
        suspended.setSuspendReason("Cooldown");
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(suspended));

        LockedException ex = assertThrows(LockedException.class,
                () -> authService.authenticate(new LoginAccountDto("flipper@example.com", "correct-password")));

        assertTrue(ex.getMessage().contains("Cooldown"));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void authenticatePropagatesBadCredentials() {
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authService.authenticate(new LoginAccountDto("flipper@example.com", "wrong-password")));
    }

    // -------------------------------------------------------------------------
    // googleSignIn
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void stubGoogleUserInfo(String email, String givenName) {
        when(restTemplate.exchange(eq("https://www.googleapis.com/oauth2/v3/userinfo"), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("email", email, "given_name", givenName), HttpStatus.OK));
    }

    @Test
    void googleSignInLogsInExistingUser() throws Exception {
        stubGoogleUserInfo("flipper@example.com", "Flipper");
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(activeUser()));

        GoogleSignInResult result = authService.googleSignIn("google-token");

        assertFalse(result.isNewUser());
        assertEquals("flipper@example.com", result.account().getEmail());
    }

    @Test
    void googleSignInCreatesNewUserWithVerifiedEmail() throws Exception {
        stubGoogleUserInfo("newflipper@example.com", "New");
        when(accountRepository.findAccountByEmail("newflipper@example.com")).thenReturn(Optional.empty());
        when(accountService.generateIdentifierCode(anyString())).thenReturn("0001");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(accountRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });
        when(collectionRepository.save(any(Collection.class))).thenAnswer(invocation -> {
            Collection c = invocation.getArgument(0);
            c.setId(9L);
            return c;
        });

        GoogleSignInResult result = authService.googleSignIn("google-token");

        assertTrue(result.isNewUser());
        assertTrue(((User) result.account()).getEmailVerified());
    }

    @Test
    void googleSignInRejectsUnsuccessfulResponse() {
        when(restTemplate.exchange(eq("https://www.googleapis.com/oauth2/v3/userinfo"), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

        Exception ex = assertThrows(Exception.class, () -> authService.googleSignIn("bad-token"));
        assertEquals("Invalid Google access token.", ex.getMessage());
    }

    @Test
    void googleSignInRejectsAdminBootstrapEmail() {
        ReflectionTestUtils.setField(authService, "adminBootstrapEmail", "admin@example.com");
        stubGoogleUserInfo("admin@example.com", "Admin");

        Exception ex = assertThrows(Exception.class, () -> authService.googleSignIn("google-token"));
        assertTrue(ex.getMessage().contains("cannot sign in with Google"));
    }

    // -------------------------------------------------------------------------
    // forgotPassword / confirmForgotPassword
    // -------------------------------------------------------------------------

    @Test
    void forgotPasswordSendsResetCodeForKnownEmail() throws Exception {
        User user = activeUser();
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(user));

        authService.forgotPassword("flipper@example.com");

        verify(emailTokenRepository).deleteByOwner_Id(1L);
        verify(emailTokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendEmail(eq("flipper@example.com"), anyString(), anyString());
    }

    @Test
    void forgotPasswordDoesNothingForUnknownEmail() throws Exception {
        when(accountRepository.findAccountByEmail("nobody@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword("nobody@example.com");

        verify(emailTokenRepository, never()).save(any());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void confirmForgotPasswordRejectsShortPassword() {
        Exception ex = assertThrows(Exception.class, () -> authService.confirmForgotPassword(
                new ConfirmForgotPasswordDto("flipper@example.com", "123456", "short")));
        assertEquals("Password must be at least 7 characters.", ex.getMessage());
    }

    @Test
    void confirmForgotPasswordSucceeds() throws Exception {
        User user = activeUser();
        EmailVerificationToken token = new EmailVerificationToken(user);
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(user));
        when(emailTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new-password")).thenReturn("hashed-new");

        authService.confirmForgotPassword(new ConfirmForgotPasswordDto("flipper@example.com", "123456", "new-password"));

        verify(emailTokenRepository).delete(token);
        assertEquals("hashed-new", user.getPassword());
    }

    @Test
    void confirmForgotPasswordRejectsExpiredCode() {
        User user = activeUser();
        EmailVerificationToken token = new EmailVerificationToken(user);
        token.setExpiration(Instant.now().minusSeconds(60));
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(user));
        when(emailTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));

        Exception ex = assertThrows(Exception.class, () -> authService.confirmForgotPassword(
                new ConfirmForgotPasswordDto("flipper@example.com", "123456", "new-password")));
        assertEquals("Code has expired. Please request a new one.", ex.getMessage());
    }

    @Test
    void confirmForgotPasswordRejectsTokenBelongingToDifferentAccount() {
        User user = activeUser();
        User otherUser = activeUser();
        otherUser.setId(2L);
        EmailVerificationToken token = new EmailVerificationToken(otherUser);
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(user));
        when(emailTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));

        Exception ex = assertThrows(Exception.class, () -> authService.confirmForgotPassword(
                new ConfirmForgotPasswordDto("flipper@example.com", "123456", "new-password")));
        assertEquals("Invalid code.", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // sendAdminLoginCode / verifyAdminLoginCode
    // -------------------------------------------------------------------------

    @Test
    void sendAdminLoginCodeSavesTokenAndSendsEmail() throws Exception {
        User admin = activeUser();
        admin.setRole("ADMIN");

        authService.sendAdminLoginCode(admin);

        verify(emailTokenRepository).deleteByOwner_Id(1L);
        verify(emailTokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendEmail(eq("flipper@example.com"), eq("Balisong Flipping Hub — Admin Login Code"), anyString());
    }

    @Test
    void verifyAdminLoginCodeSucceedsAndMarksEmailVerified() throws Exception {
        User admin = activeUser();
        admin.setRole("ADMIN");
        admin.setEmailVerified(false);
        EmailVerificationToken token = new EmailVerificationToken(admin);
        when(emailTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));

        Account result = authService.verifyAdminLoginCode("flipper@example.com", "123456");

        assertEquals(admin, result);
        verify(accountRepository).save(admin);
        verify(emailTokenRepository).delete(token);
        verify(emailService).sendEmail(eq("flipper@example.com"), eq("Balisong Flipping Hub — Admin Login Successful"), anyString());
    }

    @Test
    void verifyAdminLoginCodeRejectsNonAdminAccount() {
        User user = activeUser();
        user.setRole("USER");
        EmailVerificationToken token = new EmailVerificationToken(user);
        when(emailTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));

        Exception ex = assertThrows(Exception.class, () -> authService.verifyAdminLoginCode("flipper@example.com", "123456"));
        assertEquals("Invalid code.", ex.getMessage());
    }

    @Test
    void verifyAdminLoginCodeRejectsEmailMismatch() {
        User admin = activeUser();
        admin.setRole("ADMIN");
        EmailVerificationToken token = new EmailVerificationToken(admin);
        when(emailTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));

        Exception ex = assertThrows(Exception.class, () -> authService.verifyAdminLoginCode("someoneelse@example.com", "123456"));
        assertEquals("Invalid code.", ex.getMessage());
    }

    @Test
    void verifyAdminLoginCodeRejectsExpiredToken() {
        User admin = activeUser();
        admin.setRole("ADMIN");
        EmailVerificationToken token = new EmailVerificationToken(admin);
        token.setExpiration(Instant.now().minusSeconds(60));
        when(emailTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));

        Exception ex = assertThrows(Exception.class, () -> authService.verifyAdminLoginCode("flipper@example.com", "123456"));
        assertEquals("Code has expired. Please log in again.", ex.getMessage());
    }
}
