package com.yourproject.tcm.service;

import org.springframework.stereotype.Service;

@Service
public class EmailService {

    public void sendOtpEmail(String toEmail, String otpCode) {
        // In a real application, this would use JavaMailSender
        System.out.println("==================================================");
        System.out.println("EMAIL SIMULATION");
        System.out.println("To: " + toEmail);
        System.out.println("Subject: Your Verification Code");
        System.out.println("Body: Your OTP code for TCM registration is: " + otpCode);
        System.out.println("==================================================");
    }
}
