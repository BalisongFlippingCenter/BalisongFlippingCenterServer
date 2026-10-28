package com.example.BalisongFlipping.services;



public interface EmailService {
    
    void sendEmail(String to, String subject, String text); 

    Boolean validateEmailTokenVerification(String emailToken) throws Exception; 
}
