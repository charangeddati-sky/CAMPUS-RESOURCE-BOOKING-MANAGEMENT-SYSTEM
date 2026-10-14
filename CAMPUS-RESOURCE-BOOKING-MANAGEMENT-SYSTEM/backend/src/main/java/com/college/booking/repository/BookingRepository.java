package com.college.booking.repository;

import com.college.booking.entity.Booking;
import com.college.booking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserIdOrderByBookingDateDescStartTimeDesc(Long userId);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByBookingDate(LocalDate date);

    long countByBookingDate(LocalDate date);

    long countByStatus(BookingStatus status);

    long countByUserIdAndStatusIn(Long userId, Collection<BookingStatus> statuses);

    long countByBookingDateBetween(LocalDate from, LocalDate to);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.user
            WHERE b.user.id = :userId
              AND b.status IN :statuses
              AND (b.bookingDate > :today OR (b.bookingDate = :today AND b.endTime >= :now))
            ORDER BY b.bookingDate ASC, b.startTime ASC
            """)
    List<Booking> findUpcoming(
            @Param("userId") Long userId,
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("today") LocalDate today,
            @Param("now") LocalTime now
    );

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.user
            WHERE b.user.id = :userId
            ORDER BY b.bookingDate DESC, b.startTime DESC
            """)
    List<Booking> findRecentByUser(@Param("userId") Long userId);

    Optional<Booking> findByCheckInToken(String token);

    @Query("""
            SELECT DISTINCT b FROM Booking b
            JOIN BookingResource br ON br.booking = b
            WHERE br.resource.id = :resourceId
              AND b.bookingDate = :date
              AND b.status IN :statuses
              AND b.startTime < :endTime
              AND b.endTime > :startTime
            """)
    List<Booking> findConflicts(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") Collection<BookingStatus> statuses
    );

    @Query("""
            SELECT DISTINCT b FROM Booking b
            JOIN BookingResource br ON br.booking = b
            WHERE br.resource.id = :resourceId
              AND b.bookingDate = :date
              AND b.status IN :statuses
            ORDER BY b.startTime
            """)
    List<Booking> findByResourceAndDate(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date,
            @Param("statuses") Collection<BookingStatus> statuses
    );

    @Query("""
            SELECT b FROM Booking b
            WHERE b.user.id = :userId
              AND b.bookingDate = :date
              AND b.status IN :statuses
              AND b.startTime < :endTime
              AND b.endTime > :startTime
            """)
    List<Booking> findUserDoubleBookings(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") Collection<BookingStatus> statuses
    );

    @Query("""
            SELECT b FROM Booking b
            WHERE b.status = :status
              AND b.bookingDate = :date
              AND b.startTime <= :cutoff
            """)
    List<Booking> findNoShowCandidates(
            @Param("status") BookingStatus status,
            @Param("date") LocalDate date,
            @Param("cutoff") LocalTime cutoff
    );

    @Query("""
            SELECT COUNT(b) FROM Booking b
            JOIN BookingResource br ON br.booking = b
            WHERE br.resource.building.id = :buildingId
              AND b.bookingDate = :date
              AND b.status IN :statuses
            """)
    long countBuildingBookingsOnDate(
            @Param("buildingId") Long buildingId,
            @Param("date") LocalDate date,
            @Param("statuses") Collection<BookingStatus> statuses
    );

    @Query("""
            SELECT b FROM Booking b
            WHERE b.bookingDate BETWEEN :from AND :to
            """)
    List<Booking> findBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT br.resource.id, b.status FROM BookingResource br
            JOIN br.booking b
            WHERE b.bookingDate = :date
              AND b.status IN :statuses
              AND b.startTime <= :now
              AND b.endTime > :now
            """)
    List<Object[]> findOccupiedNow(
            @Param("date") LocalDate date,
            @Param("now") LocalTime now,
            @Param("statuses") Collection<BookingStatus> statuses
    );

    @Query("""
            SELECT DISTINCT br.resource.id FROM BookingResource br
            JOIN br.booking b
            WHERE br.resource.id IN :resourceIds
              AND b.bookingDate = :date
              AND b.status IN :statuses
              AND b.startTime < :endTime
              AND b.endTime > :startTime
            """)
    List<Long> findBusyResourceIds(
            @Param("resourceIds") Collection<Long> resourceIds,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") Collection<BookingStatus> statuses
    );

    @Query("""
            SELECT b.status, COUNT(b) FROM Booking b
            WHERE b.bookingDate BETWEEN :from AND :to
            GROUP BY b.status
            """)
    List<Object[]> countStatusBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT b.bookingDate, COUNT(b) FROM Booking b
            WHERE b.bookingDate BETWEEN :from AND :to
            GROUP BY b.bookingDate
            ORDER BY b.bookingDate
            """)
    List<Object[]> countByDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT COUNT(DISTINCT b.user.id) FROM Booking b
            WHERE b.bookingDate BETWEEN :from AND :to
              AND b.status NOT IN :excluded
            """)
    long countActiveUsersBetween(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("excluded") Collection<BookingStatus> excluded
    );

    @Query("""
            SELECT b.user.role, COUNT(DISTINCT b.user.id) FROM Booking b
            WHERE b.bookingDate BETWEEN :from AND :to
              AND b.status NOT IN :excluded
            GROUP BY b.user.role
            """)
    List<Object[]> countActiveUsersByRole(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("excluded") Collection<BookingStatus> excluded
    );
}
