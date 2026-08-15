package com.routex.service;

import com.routex.dto.AuthDTOs.AuthResponse;
import com.routex.dto.AuthDTOs.LoginRequest;
import com.routex.dto.AuthDTOs.OtpVerifyRequest;
import com.routex.dto.AuthDTOs.RegisterRequest;
import com.routex.entity.User;
import com.routex.repository.UserRepository;
import com.routex.security.JwtUtil;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthService {
    private static final String DEMO_OTP = "123456";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        String otp = generateOtp();
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(User.Role.USER)
                .enabled(false)
                .otp(otp)
                .otpExpiry(LocalDateTime.now().plusMinutes(10))
                .build();

        userRepository.save(user);

        try {
            emailService.sendOtpEmail(request.getEmail(), otp);
            return "Registration successful! Please check your email for OTP.";
        } catch (Exception e) {
            // Email failed — auto-enable user so they can log in directly
            System.out.println("=== EMAIL FAILED. Auto-enabling user: " + request.getEmail() + " OTP was: " + otp + " ===");
            user.setEnabled(true);
            user.setOtp(null);
            user.setOtpExpiry(null);
            userRepository.save(user);
            return "Registration successful! You can now login directly.";
        }
    }

    public String verifyOtp(OtpVerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (isDemoAccount(user.getEmail()) && DEMO_OTP.equals(request.getOtp())) {
            user.setEnabled(true);
            user.setOtp(null);
            user.setOtpExpiry(null);
            userRepository.save(user);
            return "Demo OTP verified successfully!";
        }

        if (!request.getOtp().equals(user.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired. Please register again.");
        }

        user.setEnabled(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        return "Email verified successfully! You can now log in.";
    }

    @Transactional
    public String resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (isDemoAccount(email)) {
            return "Demo OTP is " + DEMO_OTP;
        }

        if (user.isEnabled()) {
            throw new RuntimeException("Account is already verified");
        }

        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        emailService.sendOtpEmail(email, otp);
        return "New OTP sent to your email.";
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedFalse(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("Please verify your email first");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getEmail(), user.getFullName(), user.getRole().name());
    }

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    private boolean isDemoAccount(String email) {
        return "user@routex.lk".equalsIgnoreCase(email)
                || "admin@routex.lk".equalsIgnoreCase(email);
    }
}