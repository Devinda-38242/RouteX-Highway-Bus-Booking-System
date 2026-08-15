package com.routex.controller;

import com.routex.dto.*;
import com.routex.dto.BookingDTOs.BookingResponse;
import com.routex.entity.BusRoute;
import com.routex.entity.User;
import com.routex.repository.UserRepository;
import com.routex.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-only management endpoints")
public class AdminController {

    private final BusRouteService routeService;
    private final BookingService bookingService;
    private final UserRepository userRepository;

    // ----- Route Management -----

    @GetMapping("/routes")
    public ResponseEntity<ApiResponse<Page<BusRoute>>> getAllRoutes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ResponseEntity.ok(ApiResponse.success(routeService.getAll(pageable), "All routes"));
    }

    @PostMapping("/routes")
    @Operation(summary = "Add a new bus route")
    public ResponseEntity<ApiResponse<BusRoute>> addRoute(@Valid @RequestBody BusRoute route) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(routeService.save(route), "Route created"));
    }

    @PutMapping("/routes/{id}")
    public ResponseEntity<ApiResponse<BusRoute>> updateRoute(@PathVariable Long id,
                                                              @Valid @RequestBody BusRoute route) {
        route.setId(id);
        return ResponseEntity.ok(ApiResponse.success(routeService.save(route), "Route updated"));
    }

    @DeleteMapping("/routes/{id}")
    public ResponseEntity<ApiResponse<String>> deleteRoute(@PathVariable Long id) {
        routeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Route deactivated"));
    }

    // ----- Booking Management -----

    @GetMapping("/bookings")
    @Operation(summary = "Get all bookings with pagination")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(bookingService.getAllBookings(pageable), "All bookings"));
    }

    // ----- User Management -----

    @GetMapping("/users")
    @Operation(summary = "Get all users with pagination")
    public ResponseEntity<ApiResponse<Page<UserAdminResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> usersPage = userRepository.findByDeletedFalse(pageable);
        Page<UserAdminResponse> responsePage = usersPage.map(UserAdminResponse::new);
        return ResponseEntity.ok(ApiResponse.success(responsePage, "All users"));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Update user details")
    public ResponseEntity<ApiResponse<UserAdminResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        
        User saved = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(new UserAdminResponse(saved), "User updated successfully"));
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Update user role")
    public ResponseEntity<ApiResponse<UserAdminResponse>> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String roleStr = body.get("role");
        if (roleStr == null) {
            throw new RuntimeException("Role is required");
        }
        
        User.Role newRole;
        try {
            newRole = User.Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role");
        }

        if (userDetails != null && user.getEmail().equalsIgnoreCase(userDetails.getUsername())) {
            if (newRole == User.Role.USER && user.getRole() == User.Role.ADMIN) {
                throw new RuntimeException("You cannot demote your own account");
            }
        }

        user.setRole(newRole);
        User saved = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(new UserAdminResponse(saved), "User role updated successfully"));
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "Update user status")
    public ResponseEntity<ApiResponse<UserAdminResponse>> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            throw new RuntimeException("Enabled status is required");
        }

        if (userDetails != null && user.getEmail().equalsIgnoreCase(userDetails.getUsername())) {
            if (!enabled) {
                throw new RuntimeException("You cannot disable your own account");
            }
        }

        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(new UserAdminResponse(saved), "User status updated successfully"));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete user")
    public ResponseEntity<ApiResponse<String>> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isDeleted()) {
            throw new RuntimeException("User is already deleted");
        }

        if (userDetails != null && user.getEmail().equalsIgnoreCase(userDetails.getUsername())) {
            throw new RuntimeException("You cannot delete your own account");
        }

        if (user.getRole() == User.Role.ADMIN) {
            long activeAdmins = userRepository.findAll().stream()
                    .filter(u -> !u.isDeleted() && u.getRole() == User.Role.ADMIN)
                    .count();
            if (activeAdmins <= 1) {
                throw new RuntimeException("Cannot delete the last remaining admin account");
            }
        }

        user.setDeleted(true);
        user.setEnabled(false);
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }

    @lombok.Data
    public static class UserUpdateRequest {
        @jakarta.validation.constraints.NotBlank(message = "Full name is required")
        private String fullName;

        @jakarta.validation.constraints.NotBlank(message = "Email is required")
        @jakarta.validation.constraints.Email(message = "Invalid email format")
        private String email;

        @jakarta.validation.constraints.Pattern(regexp = "^0[0-9]{9}$", message = "Invalid Sri Lankan phone number")
        private String phone;
    }
}
