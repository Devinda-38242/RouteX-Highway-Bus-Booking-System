package com.routex.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "schedules",
       uniqueConstraints = @UniqueConstraint(columnNames = {"bus_route_id","travel_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_route_id", nullable = false)
    private BusRoute busRoute;

    @Column(nullable = false)
    private LocalDate travelDate;

    // Seats that are booked (comma-separated seat labels like "A1,A2,B3")
    @Column(length = 1000)
    private String bookedSeats = "";

    private int availableSeats;

    @Enumerated(EnumType.STRING)
    private ScheduleStatus status = ScheduleStatus.OPEN;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL)
    private List<Booking> bookings = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum ScheduleStatus {
        OPEN, FULL, CANCELLED
    }

    // Helper to get list of booked seat labels
    public List<String> getBookedSeatList() {
        if (bookedSeats == null || bookedSeats.isBlank()) return new ArrayList<>();
        return new ArrayList<>(List.of(bookedSeats.split(",")));
    }
}
