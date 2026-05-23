package com.example.gearshop.service;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetService {

    private static final int OTP_LENGTH = 6;
    private static final String DIGITS = "0123456789";

    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.mail.username:no-reply@gearshop.local}")
    private String fromEmail;

    public PasswordResetService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Sinh OTP 6 so ngau nhien bang SecureRandom de tang do an toan.
    public String generateVerificationCode() {
        StringBuilder code = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            int index = secureRandom.nextInt(DIGITS.length());
            code.append(DIGITS.charAt(index));
        }
        return code.toString();
    }

    // Tao va gui email xac nhan dang ky tai khoan.
    public void sendRegistrationVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Ma xac nhan dang ky tai khoan - PGearShop");
        message.setText("""
            Xin chao,

            Ban vua tao yeu cau dang ky tai khoan PGearShop.
            Ma xac nhan cua ban la: %s
            Ma co hieu luc trong 5 phut.

            Neu ban khong thuc hien yeu cau nay, vui long bo qua email nay.

            PGearShop
            """.formatted(code));

        mailSender.send(message);
    }

    // Tao va gui email chua ma xac nhan dat lai mat khau.
    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Ma xac nhan dat lai mat khau - PGearShop");
        message.setText("""
            Xin chao,

            Ban vua yeu cau dat lai mat khau tai khoan PGearShop.
            Ma xac nhan cua ban la: %s
            Ma co hieu luc trong 5 phut.

            Neu ban khong thuc hien yeu cau nay, vui long bo qua email nay.

            PGearShop
            """.formatted(code));

        mailSender.send(message);
    }
}
