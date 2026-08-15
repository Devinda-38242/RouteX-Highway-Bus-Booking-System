package com.routex.repository;

import com.routex.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    Optional<Schedule> findByBusRouteIdAndTravelDate(Long routeId, LocalDate travelDate);
    List<Schedule> findByTravelDate(LocalDate date);
}
