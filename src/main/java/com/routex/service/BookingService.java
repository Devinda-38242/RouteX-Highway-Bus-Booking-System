package com.routex.service;

import com.routex.dto.BookingDTOs.*;
import com.routex.entity.*;
import com.routex.qr.QRCodeService;
import com.routex.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final QRCodeService qrCodeService;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;

    @SuppressWarnings("ExtractMethodRecommender")
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, String userEmail) {
        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        if (schedule.getStatus() == Schedule.ScheduleStatus.CANCELLED) {
            throw new RuntimeException("This schedule has been cancelled");
        }

        // Check seats not already booked
        List<String> currentlyBooked = schedule.getBookedSeatList();
        List<String> requestedSeats = request.getSeats();

        for (String seat : requestedSeats) {
            if (currentlyBooked.contains(seat)) {
                throw new RuntimeException("Seat " + seat + " is already booked. Please select another seat.");
            }
        }

        // Check capacity
        if (requestedSeats.size() > schedule.getAvailableSeats()) {
            throw new RuntimeException("Not enough seats available");
        }

        // Calculate total
        BigDecimal price = schedule.getBusRoute().getPrice();
        BigDecimal total = price.multiply(BigDecimal.valueOf(requestedSeats.size()));

        // Generate unique booking reference
        String bookingRef = "RTX" + System.currentTimeMillis();

        // Generate QR code
        BusRoute route = schedule.getBusRoute();
        String routeStr = route.getOrigin() + " → " + route.getDestination();
        String qrContent = qrCodeService.buildQRContent(
                bookingRef, request.getPassengerName(), routeStr,
                schedule.getTravelDate().toString(),
                String.join(",", requestedSeats)
        );
        String qrBase64 = qrCodeService.generateQRCodeBase64(qrContent);

        // Save booking
        User user = userEmail != null ? userRepository.findByEmail(userEmail).orElse(null) : null;
        Booking booking = Booking.builder()
                .bookingReference(bookingRef)
                .user(user)
                .schedule(schedule)
                .selectedSeats(String.join(",", requestedSeats))
                .passengerName(request.getPassengerName())
                .passengerEmail(request.getPassengerEmail())
                .passengerPhone(request.getPassengerPhone())
                .totalAmount(total)
                .status(Booking.BookingStatus.CONFIRMED)
                .paymentStatus(Booking.PaymentStatus.PAID)
                .qrCode(qrBase64)
                .build();
        bookingRepository.save(booking);

        // Update schedule
        List<String> updated = new ArrayList<>(currentlyBooked);
        updated.addAll(requestedSeats);
        schedule.setBookedSeats(String.join(",", updated));
        schedule.setAvailableSeats(schedule.getAvailableSeats() - requestedSeats.size());
        if (schedule.getAvailableSeats() == 0) {
            schedule.setStatus(Schedule.ScheduleStatus.FULL);
        }
        scheduleRepository.save(schedule);

        // Broadcast seat update via WebSocket
        SeatStatusResponse seatStatus = buildSeatStatus(schedule);
        messagingTemplate.convertAndSend("/topic/seats/" + schedule.getId(), seatStatus);

        // Send confirmation email
        emailService.sendBookingConfirmation(
                request.getPassengerEmail(), request.getPassengerName(),
                bookingRef, routeStr, schedule.getTravelDate().toString(),
                String.join(", ", requestedSeats), total.toString(), qrBase64
        );

        return toResponse(booking, route, schedule);
    }

    public SeatStatusResponse getSeatStatus(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
        return buildSeatStatus(schedule);
    }

    public Page<BookingResponse> getUserBookings(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return bookingRepository.findByUserIdAndDeletedFalse(user.getId(), pageable)
                .map(b -> toResponse(b, b.getSchedule().getBusRoute(), b.getSchedule()));
    }

    public BookingResponse getByRef(String ref) {
        Booking b = bookingRepository.findByBookingReference(ref)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        return toResponse(b, b.getSchedule().getBusRoute(), b.getSchedule());
    }

    @Transactional
    public String cancelBooking(String ref, String userEmail) {
        Booking b = bookingRepository.findByBookingReference(ref)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (b.getUser() != null && !b.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Access denied");
        }
        if (b.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking is already cancelled");
        }

        b.setStatus(Booking.BookingStatus.CANCELLED);
        b.setPaymentStatus(Booking.PaymentStatus.REFUNDED);
        bookingRepository.save(b);

        // Free up seats
        Schedule schedule = b.getSchedule();
        List<String> booked = new ArrayList<>(schedule.getBookedSeatList());
        booked.removeAll(Arrays.asList(b.getSelectedSeats().split(",")));
        schedule.setBookedSeats(String.join(",", booked));
        schedule.setAvailableSeats(schedule.getAvailableSeats() + b.getSelectedSeats().split(",").length);
        schedule.setStatus(Schedule.ScheduleStatus.OPEN);
        scheduleRepository.save(schedule);

        messagingTemplate.convertAndSend("/topic/seats/" + schedule.getId(), buildSeatStatus(schedule));
        return "Booking cancelled successfully";
    }

    // --- Admin ---
    public Page<BookingResponse> getAllBookings(Pageable pageable) {
        return bookingRepository.findByDeletedFalse(pageable)
                .map(b -> toResponse(b, b.getSchedule().getBusRoute(), b.getSchedule()));
    }

    private SeatStatusResponse buildSeatStatus(Schedule schedule) {
        SeatStatusResponse r = new SeatStatusResponse();
        r.setScheduleId(schedule.getId());
        r.setBookedSeats(schedule.getBookedSeatList());
        r.setAvailableSeats(schedule.getAvailableSeats());
        r.setTotalSeats(schedule.getBusRoute().getTotalSeats());
        return r;
    }

    private BookingResponse toResponse(Booking b, BusRoute route, Schedule schedule) {
        BookingResponse r = new BookingResponse();
        r.setId(b.getId());
        r.setBookingReference(b.getBookingReference());
        r.setOrigin(route.getOrigin());
        r.setDestination(route.getDestination());
        r.setTravelDate(schedule.getTravelDate());
        r.setDepartureTime(route.getDepartureTime());
        r.setArrivalTime(route.getArrivalTime());
        r.setOperatorName(route.getOperatorName());
        r.setPassengerName(b.getPassengerName());
        r.setPassengerEmail(b.getPassengerEmail());
        r.setPassengerPhone(b.getPassengerPhone());
        r.setSeats(Arrays.asList(b.getSelectedSeats().split(",")));
        r.setTotalAmount(b.getTotalAmount());
        r.setStatus(b.getStatus().name());
        r.setPaymentStatus(b.getPaymentStatus().name());
        r.setQrCode(b.getQrCode());
        r.setCreatedAt(b.getCreatedAt() != null ?
                b.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
        return r;
    }
}
