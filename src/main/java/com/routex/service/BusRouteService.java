package com.routex.service;

import com.routex.entity.*;
import com.routex.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
@Service
@RequiredArgsConstructor
public class BusRouteService {

    private final BusRouteRepository routeRepository;
    private final ScheduleRepository scheduleRepository;

    /**
     * Returns combined list of all unique city names (origins + destinations)
     * so both From and To dropdowns show every city in the system.
     */
    public Map<String, List<String>> getLocations() {
        List<String> origins      = routeRepository.findAllOrigins();
        List<String> destinations = routeRepository.findAllDestinations();

        // Merge and deduplicate so every city appears in both dropdowns
        List<String> allCities = Stream.concat(origins.stream(), destinations.stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        Map<String, List<String>> locations = new HashMap<>();
        locations.put("origins",      allCities);   // all cities for From dropdown
        locations.put("destinations", allCities);   // all cities for To dropdown
        return locations;
    }

    /**
     * Search routes by exact origin/destination (case-insensitive).
     * Falls back to fuzzy LIKE search if exact match returns nothing
     * (handles partial input like "Colombo" vs "Colombo (Pettah)").
     */
    public Page<BusRoute> searchRoutes(String origin, String destination, Pageable pageable) {
        Page<BusRoute> exact = routeRepository.searchRoutes(origin.trim(), destination.trim(), pageable);
        if (exact.getTotalElements() > 0) return exact;

        // Fuzzy fallback for partial city names typed by user
        return routeRepository.searchRoutesFuzzy(origin.trim(), destination.trim(), pageable);
    }

    @Transactional
    public Schedule getOrCreateSchedule(Long routeId, LocalDate date) {
        return scheduleRepository.findByBusRouteIdAndTravelDate(routeId, date)
                .orElseGet(() -> {
                    BusRoute route = routeRepository.findById(routeId)
                            .orElseThrow(() -> new RuntimeException("Route not found"));
                    Schedule schedule = Schedule.builder()
                            .busRoute(route)
                            .travelDate(date)
                            .availableSeats(route.getTotalSeats())
                            .bookedSeats("")
                            .status(Schedule.ScheduleStatus.OPEN)
                            .build();
                    return scheduleRepository.save(schedule);
                });
    }

    @SuppressWarnings("unused")
    public Schedule getSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
    }

    // Admin CRUD
    public BusRoute save(BusRoute route) { return routeRepository.save(route); }
    public void delete(Long id) {
        BusRoute r = routeRepository.findById(id).orElseThrow();
        r.setActive(false);
        routeRepository.save(r);
    }
    public List<BusRoute> getAll() { return routeRepository.findAll(); }
    public Page<BusRoute> getAll(Pageable pageable) { return routeRepository.findAll(pageable); }
    public BusRoute getById(Long id) {
        return routeRepository.findById(id).orElseThrow(() -> new RuntimeException("Route not found"));
    }
}
