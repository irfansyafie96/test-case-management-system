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

    public void sendInvitationEmail(String toEmail, String inviteLink, String orgName) {
        System.out.println("==================================================");
        System.out.println("EMAIL SIMULATION - TEAM INVITATION");
        System.out.println("To: " + toEmail);
        System.out.println("Subject: You've been invited to join " + orgName);
        System.out.println("Body: Click the link below to join the team:");
        System.out.println(inviteLink);
        System.out.println("==================================================");
    }
}
