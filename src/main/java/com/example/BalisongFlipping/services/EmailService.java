package com.example.BalisongFlipping.services;


import com.example.BalisongFlipping.modals.tokens.EmailVerificationToken;

public interface EmailService {
    
    void sendEmail(String to, String subject, String text); 

    EmailVerificationToken createNewEmailVerificationToken(String userEmail) throws Exception; 
    EmailVerificationToken createReplacementEmailVerificationToken(String email) throws Exception;
    Boolean validateEmailTokenVerification(String emailToken) throws Exception; 
}
