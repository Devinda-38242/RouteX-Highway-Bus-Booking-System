package com.routex.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BookingDTOs {

    @Data
    public static class CreateBookingRequest {
        @NotNull private Long scheduleId;
        @NotEmpty private List<String> seats;
        @NotBlank private String passengerName;
        @Email @NotBlank private String passengerEmail;
        @Pattern(regexp = "^0[0-9]{9}$") private String passengerPhone;
    }

    @Data
    public static class BookingResponse {
        private Long id;
        private String bookingReference;
        private String origin;
        private String destination;
        private LocalDate travelDate;
        private String departureTime;
        private String arrivalTime;
        private String operatorName;
        private String passengerName;
        private String passengerEmail;
        private String passengerPhone;
        private List<String> seats;
        private BigDecimal totalAmount;
        private String status;
        private String paymentStatus;
        private String qrCode;
        private String createdAt;
    }

    @Data
    public static class SeatStatusResponse {
        private Long scheduleId;
        private List<String> bookedSeats;
        private int availableSeats;
        private int totalSeats;
    }
}
