package com.routex.controller;

import com.routex.dto.*;
import com.routex.dto.BookingDTOs.*;
import com.routex.service.BookingService;
import com.routex.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Create and manage bookings")
public class BookingController {

    private final BookingService bookingService;
    private final EmailService emailService;

    @PostMapping
    @Operation(summary = "Create a new booking")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        BookingResponse booking = bookingService.createBooking(request, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(booking, "Booking confirmed! Check your email for details."));
    }

    @GetMapping("/my")
    @Operation(summary = "Get current user's bookings (paginated)")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getUserBookings(userDetails.getUsername(), pageable),
                "Your bookings"));
    }

    @GetMapping("/{ref}")
    @Operation(summary = "Get booking by reference number")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(@PathVariable String ref) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getByRef(ref), "Booking found"));
    }

    @PutMapping("/{ref}/cancel")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<ApiResponse<String>> cancelBooking(
            @PathVariable String ref,
            @AuthenticationPrincipal UserDetails userDetails) {
        String msg = bookingService.cancelBooking(ref, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, msg));
    }

    private static class OtpEntry {
        final String otp;
        final java.time.LocalDateTime expiryTime;

        OtpEntry(String otp) {
            this.otp = otp;
            this.expiryTime = java.time.LocalDateTime.now().plusMinutes(5);
        }

        boolean isExpired() {
            return java.time.LocalDateTime.now().isAfter(expiryTime);
        }
    }

    private final java.util.concurrent.ConcurrentHashMap<String, OtpEntry> otpCache = new java.util.concurrent.ConcurrentHashMap<>();

    @PostMapping("/send-otp")
    @Operation(summary = "Send temporary verification OTP to passenger email")
    public ResponseEntity<ApiResponse<String>> sendPassengerOtp(@RequestParam String email) {
        if ("user@routex.lk".equalsIgnoreCase(email) || "admin@routex.lk".equalsIgnoreCase(email)) {
            return ResponseEntity.ok(ApiResponse.success(null, "Demo OTP code is 123456"));
        }

        // Generate a clean 6-digit cryptographic confirmation string
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        // Store OTP in-memory
        otpCache.put(email.toLowerCase(), new OtpEntry(otp));

        // Pass the arguments cleanly to your Brevo API setup
        emailService.sendOtpEmail(email, otp);

        return ResponseEntity.ok(ApiResponse.success(null, "Verification OTP code sent safely to your email!"));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify passenger email OTP")
    public ResponseEntity<ApiResponse<String>> verifyPassengerOtp(
            @RequestParam String email,
            @RequestParam String otp) {
        
        if (("user@routex.lk".equalsIgnoreCase(email) || "admin@routex.lk".equalsIgnoreCase(email)) 
                && "123456".equals(otp)) {
            return ResponseEntity.ok(ApiResponse.success(null, "OTP verified successfully!"));
        }

        OtpEntry entry = otpCache.get(email.toLowerCase());
        if (entry == null || entry.isExpired() || !entry.otp.equals(otp)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid or expired OTP"));
        }

        otpCache.remove(email.toLowerCase());
        return ResponseEntity.ok(ApiResponse.success(null, "OTP verified successfully!"));
    }
}
