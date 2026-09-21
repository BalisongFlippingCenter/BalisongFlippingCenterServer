package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.AdminAccountSummaryDto;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.repositories.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {

    @Mock private AccountRepository accountRepository;

    private AdminAccountService adminAccountService;

    @BeforeEach
    void setUp() {
        adminAccountService = new AdminAccountService();
        ReflectionTestUtils.setField(adminAccountService, "accountRepository", accountRepository);
    }

    private User user(Long id, String role) {
        User u = new User();
        u.setId(id);
        u.setEmail("flipper" + id + "@example.com");
        u.setDisplayName("Flipper" + id);
        u.setRole(role);
        return u;
    }

    @Test
    void searchReturnsEmptyListForBlankQuery() {
        assertTrue(adminAccountService.search("  ").isEmpty());
    }

    @Test
    void searchMapsResults() {
        when(accountRepository.searchForAdmin("flip")).thenReturn(List.of(user(1L, "USER")));
        List<AdminAccountSummaryDto> results = adminAccountService.search("flip");
        assertEquals(1, results.size());
        assertEquals("Flipper1", results.get(0).displayName());
    }

    @Test
    void getByIdReturnsAccount() throws Exception {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user(1L, "USER")));
        assertEquals("flipper1@example.com", adminAccountService.getById("1").email());
    }

    @Test
    void getByIdRejectsUnknownAccount() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());
        Exception ex = assertThrows(Exception.class, () -> adminAccountService.getById("999"));
        assertEquals("Account not found.", ex.getMessage());
    }

    @Test
    void getByIdRejectsAdminAccount() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user(1L, "ADMIN")));
        Exception ex = assertThrows(Exception.class, () -> adminAccountService.getById("1"));
        assertEquals("Admin accounts cannot be moderated.", ex.getMessage());
    }

    @Test
    void banSetsFlagAndReason() throws Exception {
        User u = user(1L, "USER");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepository.save(u)).thenReturn(u);

        AdminAccountSummaryDto result = adminAccountService.ban("1", "Repeated spam");

        assertTrue(result.banned());
        assertEquals("Repeated spam", result.banReason());
    }

    @Test
    void unbanClearsFlagAndReason() throws Exception {
        User u = user(1L, "USER");
        u.setBanned(true);
        u.setBanReason("Old reason");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepository.save(u)).thenReturn(u);

        AdminAccountSummaryDto result = adminAccountService.unban("1");

        assertFalse(result.banned());
        assertNull(result.banReason());
    }

    @Test
    void suspendSetsUntilAndReason() throws Exception {
        User u = user(1L, "USER");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepository.save(u)).thenReturn(u);
        String until = Instant.now().plusSeconds(3600).toString();

        AdminAccountSummaryDto result = adminAccountService.suspend("1", "Cooldown", until);

        assertEquals("Cooldown", result.suspendReason());
        assertEquals(until, result.suspendedUntil().toString());
    }

    @Test
    void suspendRejectsMissingUntil() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user(1L, "USER")));
        Exception ex = assertThrows(Exception.class, () -> adminAccountService.suspend("1", "Cooldown", null));
        assertEquals("until is required.", ex.getMessage());
    }

    @Test
    void suspendRejectsPastUntil() {
        User u = user(1L, "USER");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        String past = Instant.now().minusSeconds(3600).toString();

        Exception ex = assertThrows(Exception.class, () -> adminAccountService.suspend("1", "Cooldown", past));
        assertEquals("until must be in the future.", ex.getMessage());
    }

    @Test
    void suspendRejectsUnparsableUntil() {
        User u = user(1L, "USER");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));

        Exception ex = assertThrows(Exception.class, () -> adminAccountService.suspend("1", "Cooldown", "not-a-date"));
        assertEquals("until must be a valid ISO-8601 instant.", ex.getMessage());
    }

    @Test
    void unsuspendClearsUntilAndReason() throws Exception {
        User u = user(1L, "USER");
        u.setSuspendedUntil(Instant.now().plusSeconds(3600));
        u.setSuspendReason("Old reason");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepository.save(u)).thenReturn(u);

        AdminAccountSummaryDto result = adminAccountService.unsuspend("1");

        assertNull(result.suspendedUntil());
        assertNull(result.suspendReason());
    }

    @Test
    void muteSetsUntilAndReason() throws Exception {
        User u = user(1L, "USER");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepository.save(u)).thenReturn(u);
        String until = Instant.now().plusSeconds(3600).toString();

        AdminAccountSummaryDto result = adminAccountService.mute("1", "Harassment", until);

        assertEquals("Harassment", result.muteReason());
    }

    @Test
    void unmuteClearsUntilAndReason() throws Exception {
        User u = user(1L, "USER");
        u.setMutedUntil(Instant.now().plusSeconds(3600));
        u.setMuteReason("Old reason");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepository.save(u)).thenReturn(u);

        AdminAccountSummaryDto result = adminAccountService.unmute("1");

        assertNull(result.mutedUntil());
        assertNull(result.muteReason());
    }
}
