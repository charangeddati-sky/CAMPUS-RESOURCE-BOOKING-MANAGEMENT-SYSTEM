package com.college.booking.entity;

import com.college.booking.enums.WaitlistStatus;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "waitlist", indexes = {
        @Index(name = "idx_waitlist_resource", columnList = "resource_id"),
        @Index(name = "idx_waitlist_status", columnList = "status"),
        @Index(name = "idx_waitlist_slot", columnList = "resource_id,booking_date,start_time,end_time,status")
})
public class Waitlist extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WaitlistStatus status = WaitlistStatus.WAITING;

    private Instant notifiedAt;
    private Instant expiresAt;

    private Integer attendees;
    @Column(length = 400)
    private String purpose;
}
