package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.AdminAccountSummaryDto;
import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.repositories.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminAccountService {

    @Autowired
    private AccountRepository accountRepository;

    public List<AdminAccountSummaryDto> search(String query) {
        if (query == null || query.isBlank()) return List.of();
        return accountRepository.searchForAdmin(query.trim()).stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    public AdminAccountSummaryDto getById(String accountId) throws Exception {
        return toSummaryDto(requireModerable(accountId));
    }

    public AdminAccountSummaryDto ban(String accountId, String reason) throws Exception {
        Account account = requireModerable(accountId);
        account.setBanned(true);
        account.setBanReason(reason);
        return toSummaryDto(accountRepository.save(account));
    }

    public AdminAccountSummaryDto unban(String accountId) throws Exception {
        Account account = requireModerable(accountId);
        account.setBanned(false);
        account.setBanReason(null);
        return toSummaryDto(accountRepository.save(account));
    }

    public AdminAccountSummaryDto suspend(String accountId, String reason, String until) throws Exception {
        Account account = requireModerable(accountId);
        account.setSuspendedUntil(parseUntil(until));
        account.setSuspendReason(reason);
        return toSummaryDto(accountRepository.save(account));
    }

    public AdminAccountSummaryDto unsuspend(String accountId) throws Exception {
        Account account = requireModerable(accountId);
        account.setSuspendedUntil(null);
        account.setSuspendReason(null);
        return toSummaryDto(accountRepository.save(account));
    }

    public AdminAccountSummaryDto mute(String accountId, String reason, String until) throws Exception {
        Account account = requireModerable(accountId);
        account.setMutedUntil(parseUntil(until));
        account.setMuteReason(reason);
        return toSummaryDto(accountRepository.save(account));
    }

    public AdminAccountSummaryDto unmute(String accountId) throws Exception {
        Account account = requireModerable(accountId);
        account.setMutedUntil(null);
        account.setMuteReason(null);
        return toSummaryDto(accountRepository.save(account));
    }

    // admin accounts are never targetable -- step-up login + role assignment is the
    // only path to that trust level, moderation actions apply to normal users only
    private Account requireModerable(String accountId) throws Exception {
        Account account = accountRepository.findById(Long.parseLong(accountId))
                .orElseThrow(() -> new Exception("Account not found."));
        if ("ADMIN".equals(account.getRole())) {
            throw new Exception("Admin accounts cannot be moderated.");
        }
        return account;
    }

    private Instant parseUntil(String until) throws Exception {
        if (until == null || until.isBlank()) throw new Exception("until is required.");
        try {
            Instant parsed = Instant.parse(until);
            if (!parsed.isAfter(Instant.now())) throw new Exception("until must be in the future.");
            return parsed;
        } catch (java.time.format.DateTimeParseException e) {
            throw new Exception("until must be a valid ISO-8601 instant.");
        }
    }

    private AdminAccountSummaryDto toSummaryDto(Account account) {
        User user = (User) account;
        return new AdminAccountSummaryDto(
                String.valueOf(account.getId()),
                account.getEmail(),
                user.getDisplayName(),
                user.getIdentifierCode(),
                account.getRole(),
                account.getAccountCreationDate(),
                account.isBanned(),
                account.getBanReason(),
                account.getSuspendedUntil(),
                account.getSuspendReason(),
                account.getMutedUntil(),
                account.getMuteReason()
        );
    }
}
