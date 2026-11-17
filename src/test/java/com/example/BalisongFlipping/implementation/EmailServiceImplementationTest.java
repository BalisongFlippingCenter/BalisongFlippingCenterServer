package com.example.BalisongFlipping.implementation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplementationTest {

    @Mock private JavaMailSender mailSender;

    private EmailServiceImplementation emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImplementation();
        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);
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
}
