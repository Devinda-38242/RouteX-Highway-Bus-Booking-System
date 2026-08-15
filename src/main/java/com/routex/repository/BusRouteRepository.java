package com.routex.repository;

import com.routex.entity.BusRoute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusRouteRepository extends JpaRepository<BusRoute, Long> {

    // Exact match search (case-insensitive) — prevents "Colombo" matching "Makumbura (Colombo)"
    @Query("SELECT r FROM BusRoute r WHERE " +
           "LOWER(TRIM(r.origin)) = LOWER(TRIM(:origin)) AND " +
           "LOWER(TRIM(r.destination)) = LOWER(TRIM(:destination)) AND " +
           "r.active = true")
    Page<BusRoute> searchRoutes(@Param("origin") String origin,
                                @Param("destination") String destination,
                                Pageable pageable);

    // Fuzzy fallback: used when exact match returns 0 results
    @Query("SELECT r FROM BusRoute r WHERE " +
           "LOWER(r.origin) LIKE LOWER(CONCAT('%', :origin, '%')) AND " +
           "LOWER(r.destination) LIKE LOWER(CONCAT('%', :destination, '%')) AND " +
           "r.active = true")
    Page<BusRoute> searchRoutesFuzzy(@Param("origin") String origin,
                                     @Param("destination") String destination,
                                     Pageable pageable);

    List<BusRoute> findByActiveTrue();

    // Check if a specific route already exists (used by seeder to avoid duplicates)
    @Query("SELECT r FROM BusRoute r WHERE " +
           "LOWER(TRIM(r.origin)) = LOWER(TRIM(:origin)) AND " +
           "LOWER(TRIM(r.destination)) = LOWER(TRIM(:destination)) AND " +
           "r.departureTime = :departureTime AND r.active = true")
    Optional<BusRoute> findExistingRoute(@Param("origin") String origin,
                                          @Param("destination") String destination,
                                          @Param("departureTime") String departureTime);

    // Return all unique cities (both origins and destinations combined) for datalists
    @Query("SELECT DISTINCT r.origin FROM BusRoute r WHERE r.active = true ORDER BY r.origin")
    List<String> findAllOrigins();

    @Query("SELECT DISTINCT r.destination FROM BusRoute r WHERE r.active = true ORDER BY r.destination")
    List<String> findAllDestinations();
}
