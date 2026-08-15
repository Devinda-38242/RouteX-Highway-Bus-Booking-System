package com.routex.repository;

import com.routex.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String bookingReference);
    Page<Booking> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);
    List<Booking> findByScheduleId(Long scheduleId);
    Page<Booking> findByDeletedFalse(Pageable pageable); // admin
}
