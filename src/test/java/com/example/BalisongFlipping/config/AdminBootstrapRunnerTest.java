package com.example.BalisongFlipping.config;

import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.repositories.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private AccountRepository accountRepository;

    private AdminBootstrapRunner runner;

    @BeforeEach
    void setUp() {
        runner = new AdminBootstrapRunner(accountRepository);
    }

    private void setBootstrapEmail(String email) {
        ReflectionTestUtils.setField(runner, "bootstrapEmail", email);
    }

    @Test
    void doesNothingWhenBootstrapEmailIsBlank() {
        setBootstrapEmail("");

        runner.run();

        verify(accountRepository, never()).findFirstByRole(any());
        verify(accountRepository, never()).findAccountByEmail(any());
    }

    @Test
    void promotesMatchingAccountWhenNoAdminExistsYet() {
        setBootstrapEmail("admin@example.com");
        Account account = new Account();
        account.setEmail("admin@example.com");

        when(accountRepository.findFirstByRole("ADMIN")).thenReturn(Optional.empty());
        when(accountRepository.findAccountByEmail("admin@example.com")).thenReturn(Optional.of(account));

        runner.run();

        assertThat(account.getRole()).isEqualTo("ADMIN");
        verify(accountRepository).save(account);
    }

    @Test
    void doesNothingWhenNoAdminExistsAndBootstrapEmailMatchesNoAccount() {
        setBootstrapEmail("admin@example.com");

        when(accountRepository.findFirstByRole("ADMIN")).thenReturn(Optional.empty());
        when(accountRepository.findAccountByEmail("admin@example.com")).thenReturn(Optional.empty());

        runner.run();

        verify(accountRepository, never()).save(any());
    }

    @Test
    void neverDemotesExistingAdminEvenWhenBootstrapEmailDrifted() {
        setBootstrapEmail("stale-value@example.com");
        Account existingAdmin = new Account();
        existingAdmin.setEmail("real-admin@example.com");
        existingAdmin.setRole("ADMIN");

        when(accountRepository.findFirstByRole("ADMIN")).thenReturn(Optional.of(existingAdmin));

        runner.run();

        assertThat(existingAdmin.getRole()).isEqualTo("ADMIN");
        assertThat(existingAdmin.getEmail()).isEqualTo("real-admin@example.com");
        verify(accountRepository, never()).save(any());
        verify(accountRepository, never()).findAccountByEmail(any());
    }

    @Test
    void doesNothingWhenExistingAdminEmailMatchesBootstrapEmail() {
        setBootstrapEmail("real-admin@example.com");
        Account existingAdmin = new Account();
        existingAdmin.setEmail("real-admin@example.com");
        existingAdmin.setRole("ADMIN");

        when(accountRepository.findFirstByRole("ADMIN")).thenReturn(Optional.of(existingAdmin));

        runner.run();

        verify(accountRepository, never()).save(any());
    }
}
