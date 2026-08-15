package com.routex.config;

import com.routex.entity.*;
import com.routex.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final BusRouteRepository routeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedDemoUsers();
        seedAllRoutes();   // Always runs; upsertRoute prevents duplicates
    }

    // ─────────────────────────────────────────────
    //  Users
    // ─────────────────────────────────────────────
    private void seedDemoUsers() {
        upsertDemoUser("RouteX Admin",     "admin@routex.lk", "Admin@2026", null,         User.Role.ADMIN);
        upsertDemoUser("RouteX Demo User", "user@routex.lk",  "User@2026",  "0712345678", User.Role.USER);
    }

    private void upsertDemoUser(String fullName, String email, String password,
                                 String phone, User.Role role) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setRole(role);
        user.setEnabled(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        user.setDeleted(false);
        userRepository.save(user);
        log.info("Demo account ready: {} / {}", email, password);
    }

    // ─────────────────────────────────────────────
    //  Routes  — upsertRoute prevents duplicates
    // ─────────────────────────────────────────────
    private void seedAllRoutes() {
        // ── Makumbura (Colombo) ↔ Katharagama ──────────────────────────
        upsertRoute("Makumbura (Colombo)", "Katharagama", "06:30", "13:00",
                "SLTB Long Distance", "011-2612212", 1800, BusRoute.BusType.SUPER_LUXURY, "B-6", 45);
        upsertRoute("Makumbura (Colombo)", "Katharagama", "07:30", "14:00",
                "SLTB Long Distance", "071-7458751", 1800, BusRoute.BusType.SUPER_LUXURY, "B-6", 45);
        upsertRoute("Makumbura (Colombo)", "Katharagama", "12:15", "03:20",
                "SLTB Long Distance", "076-6367777", 1800, BusRoute.BusType.SUPER_LUXURY, "B-6", 45);
        upsertRoute("Makumbura (Colombo)", "Katharagama", "15:00", "05:00",
                "SLTB Long Distance", "071-9658312", 1800, BusRoute.BusType.SUPER_LUXURY, "B-6", 45);
        upsertRoute("Makumbura (Colombo)", "Katharagama", "16:15", "02:20",
                "SLTB Long Distance", "070-5906655", 1800, BusRoute.BusType.SUPER_LUXURY, "B-6", 45);
        upsertRoute("Makumbura (Colombo)", "Katharagama", "19:20", "11:15",
                "SLTB Long Distance", "071-3612890", 1800, BusRoute.BusType.SUPER_LUXURY, "B-6", 45);

        upsertRoute("Katharagama", "Makumbura (Colombo)", "13:00", "20:00",
                "SLTB Long Distance", "011-2612212", 1800, BusRoute.BusType.SUPER_LUXURY, "B-6", 45);
        upsertRoute("Katharagama", "Makumbura (Colombo)", "14:00", "21:00",
                "SLTB Long Distance", "071-7458751", 1800, BusRoute.BusType.SUPER_LUXURY, "B-6", 45);
        upsertRoute("Katharagama", "Makumbura (Colombo)", "03:20", "10:00",
                "SLTB Long Distance", "076-6367777", 1800, BusRoute.BusType.SUPER_LUXURY, "B-6", 45);
        upsertRoute("Katharagama", "Makumbura (Colombo)", "05:00", "12:00",
                "SLTB Long Distance", "071-9658312", 1800, BusRoute.BusType.SUPER_LUXURY, "B-6", 45);
        upsertRoute("Katharagama", "Makumbura (Colombo)", "02:20", "09:00",
                "SLTB Long Distance", "070-5906655", 1800, BusRoute.BusType.SUPER_LUXURY, "B-6", 45);
        upsertRoute("Katharagama", "Makumbura (Colombo)", "11:15", "18:30",
                "SLTB Long Distance", "071-3612890", 1800, BusRoute.BusType.SUPER_LUXURY, "B-6", 45);

        // ── Colombo (Pettah) ↔ Kandy ────────────────────────────────────
        upsertRoute("Colombo (Pettah)", "Kandy", "06:00", "09:30",
                "SuperLine Express", "077-1111111", 1500, BusRoute.BusType.EXPRESS, "B-11", 50);
        upsertRoute("Colombo (Pettah)", "Kandy", "09:00", "12:30",
                "LuxBus", "077-2222222", 1700, BusRoute.BusType.LUXURY, "B-11", 50);
        upsertRoute("Colombo (Pettah)", "Kandy", "14:00", "17:30",
                "Kandy Liner", "077-2223333", 1600, BusRoute.BusType.LUXURY, "B-11", 50);

        upsertRoute("Kandy", "Colombo (Pettah)", "05:30", "09:00",
                "SuperLine Express", "077-1111111", 1500, BusRoute.BusType.EXPRESS, null, 50);
        upsertRoute("Kandy", "Colombo (Pettah)", "08:30", "12:00",
                "LuxBus", "077-2222222", 1700, BusRoute.BusType.LUXURY, null, 50);
        upsertRoute("Kandy", "Colombo (Pettah)", "13:30", "17:00",
                "Kandy Liner", "077-2223333", 1600, BusRoute.BusType.LUXURY, null, 50);

        // ── Colombo (Pettah) ↔ Galle ────────────────────────────────────
        upsertRoute("Colombo (Pettah)", "Galle", "07:00", "10:00",
                "Southern Express", "077-3333333", 1200, BusRoute.BusType.EXPRESS, "B-9", 50);
        upsertRoute("Colombo (Pettah)", "Galle", "14:00", "17:00",
                "SuperLine", "077-4444444", 1400, BusRoute.BusType.LUXURY, "B-9", 50);
        upsertRoute("Colombo (Pettah)", "Galle", "19:00", "22:00",
                "Southern Express", "077-3333333", 1200, BusRoute.BusType.EXPRESS, "B-9", 50);

        upsertRoute("Galle", "Colombo (Pettah)", "05:30", "08:30",
                "Southern Express", "077-3333333", 1200, BusRoute.BusType.EXPRESS, null, 50);
        upsertRoute("Galle", "Colombo (Pettah)", "12:00", "15:00",
                "SuperLine", "077-4444444", 1400, BusRoute.BusType.LUXURY, null, 50);
        upsertRoute("Galle", "Colombo (Pettah)", "17:00", "20:00",
                "Southern Express", "077-3333333", 1200, BusRoute.BusType.EXPRESS, null, 50);

        // ── Colombo (Pettah) ↔ Jaffna ───────────────────────────────────
        upsertRoute("Colombo (Pettah)", "Jaffna", "06:30", "13:00",
                "Northern Express", "077-5555555", 2500, BusRoute.BusType.SUPER_LUXURY, "Bay 01", 50);
        upsertRoute("Colombo (Pettah)", "Jaffna", "21:00", "04:30",
                "Northern Star", "077-5556666", 2500, BusRoute.BusType.SUPER_LUXURY, "Bay 01", 50);

        upsertRoute("Jaffna", "Colombo (Pettah)", "05:00", "11:30",
                "Northern Express", "077-5555555", 2500, BusRoute.BusType.SUPER_LUXURY, null, 50);
        upsertRoute("Jaffna", "Colombo (Pettah)", "19:30", "03:00",
                "Northern Star", "077-5556666", 2500, BusRoute.BusType.SUPER_LUXURY, null, 50);

        // ── Colombo (Pettah) ↔ Matara ───────────────────────────────────
        upsertRoute("Colombo (Pettah)", "Matara", "07:00", "10:30",
                "Matara Coach", "077-6666666", 1100, BusRoute.BusType.EXPRESS, "B-10", 50);
        upsertRoute("Colombo (Pettah)", "Matara", "12:30", "16:00",
                "Matara Luxury", "077-6667777", 1300, BusRoute.BusType.LUXURY, "B-10", 50);

        upsertRoute("Matara", "Colombo (Pettah)", "05:30", "09:00",
                "Matara Coach", "077-6666666", 1100, BusRoute.BusType.EXPRESS, null, 50);
        upsertRoute("Matara", "Colombo (Pettah)", "11:00", "14:30",
                "Matara Luxury", "077-6667777", 1300, BusRoute.BusType.LUXURY, null, 50);

        // ── Colombo (Pettah) ↔ Anuradhapura ─────────────────────────────
        upsertRoute("Colombo (Pettah)", "Anuradhapura", "06:30", "11:00",
                "Heritage Lines", "077-7777777", 1600, BusRoute.BusType.LUXURY, "Bay 05", 50);
        upsertRoute("Colombo (Pettah)", "Anuradhapura", "13:00", "17:30",
                "Heritage Lines", "077-7777777", 1600, BusRoute.BusType.LUXURY, "Bay 05", 50);

        upsertRoute("Anuradhapura", "Colombo (Pettah)", "05:30", "10:00",
                "Heritage Lines", "077-7777777", 1600, BusRoute.BusType.LUXURY, null, 50);
        upsertRoute("Anuradhapura", "Colombo (Pettah)", "12:00", "16:30",
                "Heritage Lines", "077-7777777", 1600, BusRoute.BusType.LUXURY, null, 50);

        // ── Colombo (Pettah) ↔ Trincomalee ──────────────────────────────
        upsertRoute("Colombo (Pettah)", "Trincomalee", "06:00", "13:00",
                "East Coast Express", "077-8888888", 2200, BusRoute.BusType.SUPER_LUXURY, "Bay 11", 50);
        upsertRoute("Colombo (Pettah)", "Trincomalee", "20:00", "03:00",
                "East Coast Night Rider", "077-8889999", 2200, BusRoute.BusType.SUPER_LUXURY, "Bay 11", 50);

        upsertRoute("Trincomalee", "Colombo (Pettah)", "05:00", "12:00",
                "East Coast Express", "077-8888888", 2200, BusRoute.BusType.SUPER_LUXURY, null, 50);
        upsertRoute("Trincomalee", "Colombo (Pettah)", "18:00", "01:00",
                "East Coast Night Rider", "077-8889999", 2200, BusRoute.BusType.SUPER_LUXURY, null, 50);

        // ── Colombo (Pettah) ↔ Badulla ──────────────────────────────────
        upsertRoute("Colombo (Pettah)", "Badulla", "06:00", "12:00",
                "Hill Country Express", "077-9999999", 2000, BusRoute.BusType.SUPER_LUXURY, "Bay 18", 50);
        upsertRoute("Colombo (Pettah)", "Badulla", "19:30", "01:30",
                "Hill Country Night", "077-9990000", 2000, BusRoute.BusType.SUPER_LUXURY, "Bay 18", 50);

        upsertRoute("Badulla", "Colombo (Pettah)", "05:00", "11:00",
                "Hill Country Express", "077-9999999", 2000, BusRoute.BusType.SUPER_LUXURY, null, 50);
        upsertRoute("Badulla", "Colombo (Pettah)", "18:00", "00:00",
                "Hill Country Night", "077-9990000", 2000, BusRoute.BusType.SUPER_LUXURY, null, 50);

        // ── Colombo (Pettah) ↔ Batticaloa ───────────────────────────────
        upsertRoute("Colombo (Pettah)", "Batticaloa", "06:00", "13:30",
                "East Link", "076-0000001", 2300, BusRoute.BusType.SUPER_LUXURY, null, 50);
        upsertRoute("Colombo (Pettah)", "Batticaloa", "20:30", "04:00",
                "East Link Night", "076-0000002", 2300, BusRoute.BusType.SUPER_LUXURY, null, 50);

        upsertRoute("Batticaloa", "Colombo (Pettah)", "05:00", "12:30",
                "East Link", "076-0000001", 2300, BusRoute.BusType.SUPER_LUXURY, null, 50);
        upsertRoute("Batticaloa", "Colombo (Pettah)", "18:30", "02:00",
                "East Link Night", "076-0000002", 2300, BusRoute.BusType.SUPER_LUXURY, null, 50);

        log.info("✅ All routes seeded (both directions for every route)");
    }

    /**
     * Insert a route only if it doesn't already exist (origin + destination + departureTime).
     * Safe to run on every startup — no duplicate routes created.
     */
    private void upsertRoute(String origin, String destination,
                              String dep, String arr,
                              String operator, String contact,
                              int price, BusRoute.BusType type,
                              String bay, int seats) {
        boolean exists = routeRepository
                .findExistingRoute(origin, destination, dep)
                .isPresent();
        if (!exists) {
            routeRepository.save(BusRoute.builder()
                    .origin(origin).destination(destination)
                    .departureTime(dep).arrivalTime(arr)
                    .operatorName(operator).contactNumber(contact)
                    .price(BigDecimal.valueOf(price))
                    .busType(type).busBay(bay)
                    .totalSeats(seats).active(true)
                    .build());
            log.debug("Seeded: {} → {} @ {}", origin, destination, dep);
        }
    }
}
