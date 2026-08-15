package com.routex.controller;

import com.routex.dto.*;
import com.routex.dto.BookingDTOs.SeatStatusResponse;
import com.routex.entity.*;
import com.routex.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Tag(name = "Bus Routes", description = "Search and browse routes")
public class BusRouteController {

    private final BusRouteService routeService;
    private final BookingService bookingService;

    @GetMapping("/locations")
    @Operation(summary = "Get all available origin/destination locations")
    public ResponseEntity<ApiResponse<Map<String, ?>>> getLocations() {
        return ResponseEntity.ok(ApiResponse.success(routeService.getLocations(), "Locations fetched"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search routes by origin, destination and date")
    public ResponseEntity<ApiResponse<Page<BusRoute>>> searchRoutes(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "departureTime") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<BusRoute> results = routeService.searchRoutes(origin, destination, pageable);
        return ResponseEntity.ok(ApiResponse.success(results, "Search results"));
    }

    @GetMapping("/{routeId}/schedule")
    @Operation(summary = "Get or create a schedule for a route on a specific date")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSchedule(
            @PathVariable Long routeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Schedule schedule = routeService.getOrCreateSchedule(routeId, date);

        // Return DTO instead of entity to avoid Hibernate proxy serialization issue
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", schedule.getId());
        dto.put("travelDate", schedule.getTravelDate());
        dto.put("bookedSeats", schedule.getBookedSeats());
        dto.put("availableSeats", schedule.getAvailableSeats());
        dto.put("status", schedule.getStatus());

        return ResponseEntity.ok(ApiResponse.success(dto, "Schedule fetched"));
    }

    @GetMapping("/schedules/{scheduleId}/seats")
    @Operation(summary = "Get real-time seat availability for a schedule")
    public ResponseEntity<ApiResponse<SeatStatusResponse>> getSeatStatus(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getSeatStatus(scheduleId), "Seat status"));
    }
}