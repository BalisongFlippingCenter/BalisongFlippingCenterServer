package com.example.BalisongFlipping.implementation;

import com.example.BalisongFlipping.dtos.ConfirmForgotPasswordDto;
import com.example.BalisongFlipping.dtos.GoogleSignInResult;
import com.example.BalisongFlipping.dtos.LoginAccountDto;
import com.example.BalisongFlipping.dtos.RegisterAccountDto;
import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.collections.Collection;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.CollectionRepository;
import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.EmailService;
import com.example.BalisongFlipping.services.VerificationCodeService;
import com.example.BalisongFlipping.utils.TooManyRequestsException;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
    @Mock private VerificationCodeService verificationCodeService;
    @Mock private RestTemplate restTemplate;

    private AuthServiceImplementation authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImplementation(accountRepository, collectionRepository, authenticationManager, passwordEncoder);
        ReflectionTestUtils.setField(authService, "accountService", accountService);
        ReflectionTestUtils.setField(authService, "emailService", emailService);
        ReflectionTestUtils.setField(authService, "verificationCodeService", verificationCodeService);
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
        when(verificationCodeService.issue(any())).thenReturn("123456");

        Account saved = authService.signup(registerDto("flipper@example.com", "Flipper", "correct-password"));

        assertEquals("USER", saved.getRole());
        assertFalse(saved.getEmailVerified());
        verify(emailService).sendEmail(eq("flipper@example.com"), eq("Email Verification"), contains("123456"));
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
        verify(emailService, never()).sendEmail(eq("admin@example.com"), eq("Email Verification"), anyString());
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

    @Test
    void authenticateBlocksAfterTenAttemptsForTheSameEmail() {
        when(accountRepository.findAccountByEmail(anyString())).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        for (int i = 0; i < 10; i++) {
            assertThrows(BadCredentialsException.class,
                    () -> authService.authenticate(new LoginAccountDto("flipper@example.com", "wrong-password")));
        }

        assertThrows(TooManyRequestsException.class,
                () -> authService.authenticate(new LoginAccountDto("FLIPPER@example.com", "wrong-password")));
        assertThrows(BadCredentialsException.class,
                () -> authService.authenticate(new LoginAccountDto("other@example.com", "wrong-password")));
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
    // email verification
    // -------------------------------------------------------------------------

    @Test
    void reSendEmailTokenIssuesAndEmailsANewCode() throws Exception {
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(activeUser()));
        when(verificationCodeService.issue(any())).thenReturn("654321");

        authService.reSendEmailToken("flipper@example.com");

        verify(emailService).sendEmail(eq("flipper@example.com"), eq("Email Verification"), contains("654321"));
    }

    @Test
    void reSendEmailTokenRejectsUnknownEmail() {
        when(accountRepository.findAccountByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> authService.reSendEmailToken("nobody@example.com"));
        verify(verificationCodeService, never()).issue(any());
    }

    @Test
    void validateEmailVerificationChecksTheCodeAgainstThatAccount() throws Exception {
        User user = activeUser();
        user.setEmailVerified(false);
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(user));

        assertTrue(authService.validateEmailVerification("flipper@example.com", "123456"));

        verify(verificationCodeService).verify(user, "123456");
        assertTrue(user.getEmailVerified());
        verify(accountRepository).save(user);
    }

    @Test
    void validateEmailVerificationLeavesAccountUnverifiedWhenCodeIsWrong() throws Exception {
        User user = activeUser();
        user.setEmailVerified(false);
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(user));
        doThrow(new Exception("Invalid code.")).when(verificationCodeService).verify(user, "000000");

        assertThrows(Exception.class, () -> authService.validateEmailVerification("flipper@example.com", "000000"));
        assertFalse(user.getEmailVerified());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void validateEmailVerificationReturnsFalseForUnknownEmail() throws Exception {
        when(accountRepository.findAccountByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertFalse(authService.validateEmailVerification("nobody@example.com", "123456"));
        verify(verificationCodeService, never()).verify(any(), anyString());
    }

    @Test
    void validateEmailVerificationWithoutEmailFallsBackToLegacyLookup() throws Exception {
        when(emailService.validateEmailTokenVerification("123456")).thenReturn(true);

        assertTrue(authService.validateEmailVerification(null, "123456"));
        verify(verificationCodeService, never()).verify(any(), anyString());
    }

    // -------------------------------------------------------------------------
    // forgotPassword / confirmForgotPassword
    // -------------------------------------------------------------------------

    @Test
    void forgotPasswordSendsResetCodeForKnownEmail() throws Exception {
        User user = activeUser();
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(user));
        when(verificationCodeService.issue(user)).thenReturn("123456");

        authService.forgotPassword("flipper@example.com");

        verify(emailService).sendEmail(eq("flipper@example.com"), anyString(), contains("123456"));
    }

    @Test
    void forgotPasswordDoesNothingForUnknownEmail() throws Exception {
        when(accountRepository.findAccountByEmail("nobody@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword("nobody@example.com");

        verify(verificationCodeService, never()).issue(any());
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
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-password")).thenReturn("hashed-new");

        authService.confirmForgotPassword(new ConfirmForgotPasswordDto("flipper@example.com", "123456", "new-password"));

        verify(verificationCodeService).verify(user, "123456");
        assertEquals("hashed-new", user.getPassword());
    }

    @Test
    void confirmForgotPasswordKeepsOldPasswordWhenCodeIsWrong() throws Exception {
        User user = activeUser();
        user.setPassword("hashed-old");
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(user));
        doThrow(new Exception("Invalid code.")).when(verificationCodeService).verify(user, "000000");

        Exception ex = assertThrows(Exception.class, () -> authService.confirmForgotPassword(
                new ConfirmForgotPasswordDto("flipper@example.com", "000000", "new-password")));
        assertEquals("Invalid code.", ex.getMessage());
        assertEquals("hashed-old", user.getPassword());
    }

    @Test
    void confirmForgotPasswordRejectsUnknownEmail() throws Exception {
        when(accountRepository.findAccountByEmail("nobody@example.com")).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> authService.confirmForgotPassword(
                new ConfirmForgotPasswordDto("nobody@example.com", "123456", "new-password")));
        assertEquals("Invalid code.", ex.getMessage());
        verify(verificationCodeService, never()).verify(any(), anyString());
    }

    // -------------------------------------------------------------------------
    // sendAdminLoginCode / verifyAdminLoginCode
    // -------------------------------------------------------------------------

    @Test
    void sendAdminLoginCodeIssuesCodeAndSendsEmail() throws Exception {
        User admin = activeUser();
        admin.setRole("ADMIN");
        when(verificationCodeService.issue(admin)).thenReturn("123456");

        authService.sendAdminLoginCode(admin);

        verify(emailService).sendEmail(eq("flipper@example.com"), eq("Balisong Flipping Hub — Admin Login Code"), contains("123456"));
    }

    @Test
    void verifyAdminLoginCodeSucceedsAndMarksEmailVerified() throws Exception {
        User admin = activeUser();
        admin.setRole("ADMIN");
        admin.setEmailVerified(false);
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(admin));

        Account result = authService.verifyAdminLoginCode("flipper@example.com", "123456");

        assertEquals(admin, result);
        verify(verificationCodeService).verify(admin, "123456");
        verify(accountRepository).save(admin);
        verify(emailService).sendEmail(eq("flipper@example.com"), eq("Balisong Flipping Hub — Admin Login Successful"), anyString());
    }

    @Test
    void verifyAdminLoginCodeRejectsNonAdminAccount() throws Exception {
        User user = activeUser();
        user.setRole("USER");
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(user));

        Exception ex = assertThrows(Exception.class, () -> authService.verifyAdminLoginCode("flipper@example.com", "123456"));
        assertEquals("Invalid code.", ex.getMessage());
        verify(verificationCodeService, never()).verify(any(), anyString());
    }

    @Test
    void verifyAdminLoginCodeRejectsUnknownEmail() {
        when(accountRepository.findAccountByEmail("someoneelse@example.com")).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> authService.verifyAdminLoginCode("someoneelse@example.com", "123456"));
        assertEquals("Invalid code.", ex.getMessage());
    }

    @Test
    void verifyAdminLoginCodePropagatesWrongCode() throws Exception {
        User admin = activeUser();
        admin.setRole("ADMIN");
        when(accountRepository.findAccountByEmail("flipper@example.com")).thenReturn(Optional.of(admin));
        doThrow(new Exception("Invalid code.")).when(verificationCodeService).verify(admin, "000000");

        assertThrows(Exception.class, () -> authService.verifyAdminLoginCode("flipper@example.com", "000000"));
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }
}
