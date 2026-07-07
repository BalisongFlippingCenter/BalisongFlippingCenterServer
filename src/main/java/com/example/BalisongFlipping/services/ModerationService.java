package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.enums.notifications.NotificationType;
import com.example.BalisongFlipping.enums.reports.ReportReason;
import com.example.BalisongFlipping.enums.reports.ReportStatus;
import com.example.BalisongFlipping.enums.reports.TargetType;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.reports.Report;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.ReportRepository;
import com.example.BalisongFlipping.utils.ProfanityFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Random;

@Service
public class ModerationService {

    @Autowired private AccountRepository accountRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private EmailService emailService;
    @Autowired private AccountService accountService;

    @Transactional
    public void evaluate(Report report) {
        if (report.getTargetType() != TargetType.PROFILE) return;

        if (report.getReason() == ReportReason.INAPPROPRIATE_NAME) {
            evaluateName(report);
        } else if (report.getReason() == ReportReason.INAPPROPRIATE_BIO) {
            evaluateBio(report);
        }
    }

    private void evaluateName(Report report) {
        Long accountId = report.getTargetId();
        User user = accountRepository.findById(accountId).map(a -> (User) a).orElse(null);
        if (user == null) return;

        if (ProfanityFilter.containsProfanity(user.getDisplayName())) {
            String newName = "user" + (100000 + new Random().nextInt(900000));
            user.setDisplayName(newName);
            user.setIdentifierCode(accountService.generateIdentifierCode(newName));
            accountRepository.save(user);

            resolveReport(report, ReportStatus.ACTIONED);

            notificationService.sendSystem(accountId, NotificationType.NAME_RESET,
                    TargetType.PROFILE, accountId);

            emailService.sendEmail(
                    user.getEmail(),
                    "Your display name has been reset",
                    "Hi,\n\nYour display name violated our community guidelines and has been automatically reset to \"" + newName + "\".\n\n" +
                    "You can update it in your account settings.\n\nBalisong Flipping Hub"
            );
        } else {
            resolveReport(report, ReportStatus.DISMISSED);
        }
    }

    private void evaluateBio(Report report) {
        Long accountId = report.getTargetId();
        User user = accountRepository.findById(accountId).map(a -> (User) a).orElse(null);
        if (user == null) return;

        if (user.getBio() != null && ProfanityFilter.containsProfanity(user.getBio())) {
            user.setBio("");
            accountRepository.save(user);

            resolveReport(report, ReportStatus.ACTIONED);

            notificationService.sendSystem(accountId, NotificationType.BIO_CLEARED,
                    TargetType.PROFILE, accountId);

            emailService.sendEmail(
                    user.getEmail(),
                    "Your bio has been cleared",
                    "Hi,\n\nYour profile bio violated our community guidelines and has been automatically cleared.\n\n" +
                    "You can update it in your account settings.\n\nBalisong Flipping Hub"
            );
        } else {
            resolveReport(report, ReportStatus.DISMISSED);
        }
    }

    private void resolveReport(Report report, ReportStatus status) {
        report.setStatus(status);
        report.setReviewedAt(Instant.now());
        reportRepository.save(report);
    }
}
