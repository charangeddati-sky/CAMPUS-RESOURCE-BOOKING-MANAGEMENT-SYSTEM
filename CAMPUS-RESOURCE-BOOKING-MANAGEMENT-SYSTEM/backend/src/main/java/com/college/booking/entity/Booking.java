package com.college.booking.entity;

import com.college.booking.enums.BookingStatus;
import com.college.booking.enums.RecurrenceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_bookings_user", columnList = "user_id"),
        @Index(name = "idx_bookings_date", columnList = "booking_date"),
        @Index(name = "idx_bookings_status", columnList = "status"),
        @Index(name = "idx_bookings_date_time", columnList = "booking_date,start_time,end_time")
})
public class Booking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 2000)
    private String purpose;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status;

    private Integer attendees = 0;

    @Column(length = 1000)
    private String requirements;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurrenceType recurrenceType = RecurrenceType.NONE;

    private LocalDate recurrenceEndDate;

    @Column(length = 80)
    private String recurrenceRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_booking_id")
    private Booking parentBooking;

    @Column(length = 64)
    private String checkInToken;

    @Column(length = 400)
    private String rejectionReason;

    @Column(length = 40)
    private String bookingKind = "RESOURCE";
}
