package com.routex.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@SuppressWarnings({"unused"})
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String to, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("RouteX - Email Verification OTP");
            helper.setText(
                    "<h3>Email Verification</h3>" +
                            "<p>Your one-time password (OTP) is: <b>" + otp + "</b></p>" +
                            "<p>This OTP expires in 10 minutes.</p>",
                    true
            );
            mailSender.send(message);
            log.info("OTP email sent to: " + to);
        } catch (Exception e) {
            log.error("Failed to send OTP email: " + e.getMessage());
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    public void sendBookingConfirmation(String to, String name, String bookingRef,
                                        String route, String date, String seats,
                                        String total, String qrCodeBase64) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("RouteX - Booking Confirmed! #" + bookingRef);
            helper.setText(
                    "<h3>Booking Confirmed!</h3>" +
                            "<p>Hi " + name + ", your booking is confirmed!</p>" +
                            "<p><b>Booking ID:</b> " + bookingRef + "</p>" +
                            "<p><b>Route:</b> " + route + "</p>" +
                            "<p><b>Date:</b> " + date + "</p>" +
                            "<p><b>Seats:</b> " + seats + "</p>" +
                            "<p><b>Total:</b> Rs. " + total + "</p>",
                    true
            );
            mailSender.send(message);
            log.info("Booking confirmation sent to: " + to);
        } catch (Exception e) {
            log.error("Failed to send booking email: " + e.getMessage());
            throw new RuntimeException("Failed to send booking email", e);
        }
    }
}