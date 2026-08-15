package com.routex.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "bus_routes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String origin;           // e.g., "Makumbura (Colombo)"

    @NotBlank
    @Column(nullable = false)
    private String destination;      // e.g., "Katharagama"

    // Departure time string like "06:30" 
    @NotBlank
    private String departureTime;

    // Arrival time string like "13:00"
    @NotBlank
    private String arrivalTime;

    @Enumerated(EnumType.STRING)
    private BusType busType;

    // Bus operator / company name
    @NotBlank
    private String operatorName;

    // Contact number for the operator
    private String contactNumber;

    // Bus bay number e.g. "B-6"
    private String busBay;

    @DecimalMin("0.0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // Total seats in the bus (typically 45)
    @Min(1)
    @Column(nullable = false)
    private int totalSeats = 50;

    private boolean active = true;

    @JsonIgnore
    @OneToMany(mappedBy = "busRoute", cascade = CascadeType.ALL)
    private List<Schedule> schedules;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Getter
    public enum BusType {
        SUPER_LUXURY("Super Luxury"),
        LUXURY("Luxury"),
        EXPRESS("Express"),
        SUPER_EXPRESS("Super Express");

        private final String label;
        BusType(String label) { this.label = label; }
    }
}
