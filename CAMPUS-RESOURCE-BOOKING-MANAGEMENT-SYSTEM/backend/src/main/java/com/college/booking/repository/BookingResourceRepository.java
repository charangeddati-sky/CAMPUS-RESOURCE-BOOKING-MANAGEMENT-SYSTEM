package com.college.booking.repository;

import com.college.booking.entity.BookingResource;
import com.college.booking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface BookingResourceRepository extends JpaRepository<BookingResource, Long> {
    List<BookingResource> findByBookingId(Long bookingId);
    List<BookingResource> findByResourceId(Long resourceId);

    @Query("""
            SELECT br FROM BookingResource br
            JOIN FETCH br.booking b
            JOIN FETCH b.user
            JOIN FETCH br.resource r
            JOIN FETCH r.building
            JOIN FETCH r.resourceType
            WHERE b.bookingDate BETWEEN :from AND :to
            """)
    List<BookingResource> findDetailedBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT br.booking.status, COUNT(br) FROM BookingResource br
            WHERE br.resource.id = :resourceId
            GROUP BY br.booking.status
            """)
    List<Object[]> countStatusByResource(@Param("resourceId") Long resourceId);

    long countByResourceId(Long resourceId);

    @Query("""
            SELECT COUNT(br) FROM BookingResource br
            JOIN br.booking b
            WHERE br.resource.id = :resourceId
              AND b.status IN :statuses
              AND (b.bookingDate > :today OR (b.bookingDate = :today AND b.endTime > :now))
            """)
    long countUpcoming(
            @Param("resourceId") Long resourceId,
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("today") LocalDate today,
            @Param("now") java.time.LocalTime now
    );

    @Query("""
            SELECT br.resource.id, COUNT(br) FROM BookingResource br
            JOIN br.booking b
            WHERE br.resource.id IN :resourceIds
              AND b.status IN :statuses
              AND (b.bookingDate > :today OR (b.bookingDate = :today AND b.endTime > :now))
            GROUP BY br.resource.id
            """)
    List<Object[]> countUpcomingGrouped(
            @Param("resourceIds") Collection<Long> resourceIds,
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("today") LocalDate today,
            @Param("now") java.time.LocalTime now
    );

    @Query("""
            SELECT br.resource.id, COUNT(br) FROM BookingResource br
            WHERE br.resource.id IN :resourceIds
            GROUP BY br.resource.id
            """)
    List<Object[]> countAllGrouped(@Param("resourceIds") Collection<Long> resourceIds);

    @Query("""
            SELECT COUNT(br) FROM BookingResource br
            JOIN br.booking b
            WHERE b.user.id = :userId
              AND b.status IN :statuses
              AND b.bookingDate BETWEEN :from AND :to
            """)
    long countByUserBetween(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("statuses") Collection<BookingStatus> statuses
    );
}
