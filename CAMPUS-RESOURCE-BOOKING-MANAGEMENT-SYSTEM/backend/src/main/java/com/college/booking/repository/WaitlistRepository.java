package com.college.booking.repository;

import com.college.booking.entity.Waitlist;
import com.college.booking.enums.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {

    List<Waitlist> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Waitlist> findByResourceIdAndBookingDateAndStartTimeAndEndTimeAndStatusOrderByCreatedAtAsc(
            Long resourceId, LocalDate date, LocalTime start, LocalTime end, WaitlistStatus status);

    List<Waitlist> findByStatusAndExpiresAtBefore(WaitlistStatus status, Instant before);

    boolean existsByResourceIdAndUserIdAndBookingDateAndStartTimeAndEndTimeAndStatus(
            Long resourceId, Long userId, LocalDate date, LocalTime start, LocalTime end, WaitlistStatus status);

    void deleteByResourceId(Long resourceId);
}
