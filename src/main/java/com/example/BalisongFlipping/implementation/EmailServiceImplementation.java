package com.example.BalisongFlipping.implementation;

import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.modals.tokens.EmailVerificationToken;
import com.example.BalisongFlipping.repositories.EmailTokenRepository;
import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.EmailService;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Service
public class EmailServiceImplementation implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EmailTokenRepository emailTokenRepository;

    @Autowired
    private AccountService accountService;

    @Override
    public void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    @Override
    public EmailVerificationToken createNewEmailVerificationToken(String userEmail) throws Exception {
        Account foundAccount = accountService.getAccountByEmail(userEmail);
        EmailVerificationToken t = new EmailVerificationToken();
        t.setOwner(foundAccount);
        t.setExpiration(Instant.now().plusMillis(600000));
        t.setToken(t.generateToken());
        return emailTokenRepository.save(t);
    }

    @Override
    public EmailVerificationToken createReplacementEmailVerificationToken(String email) throws Exception {
        Account foundAccount = accountService.getAccountByEmail(email);
        emailTokenRepository.deleteByOwner_Id(foundAccount.getId());

        EmailVerificationToken t = new EmailVerificationToken();
        t.setOwner(foundAccount);
        t.setExpiration(Instant.now().plusMillis(600000));
        t.setToken(t.generateToken());
        return emailTokenRepository.save(t);
    }

    @Override
    public Boolean validateEmailTokenVerification(String emailToken) throws Exception {
        Optional<EmailVerificationToken> foundToken = emailTokenRepository.findByToken(emailToken);

        if (foundToken.isPresent()) {
            if (foundToken.get().getToken().equals(emailToken) &&
                    validatedEmailTokenExpiration(foundToken.get().getExpiration())) {
                accountService.verifyAccountEmail(foundToken.get().getOwner());
                return true;
            }
            return false;
        }

        throw new Exception("Token Not Found");
    }

    private Boolean validatedEmailTokenExpiration(Instant tokenExpiration) {
        return tokenExpiration.compareTo(Instant.now()) >= 0;
    }
}
