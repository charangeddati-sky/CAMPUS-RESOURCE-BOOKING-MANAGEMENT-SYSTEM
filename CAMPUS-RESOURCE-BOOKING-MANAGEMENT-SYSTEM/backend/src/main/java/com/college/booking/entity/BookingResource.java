package com.college.booking.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "booking_resources",
        indexes = {
                @Index(name = "idx_br_resource", columnList = "resource_id"),
                @Index(name = "idx_br_booking", columnList = "booking_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_booking_resource", columnNames = {"booking_id", "resource_id"})
        })
public class BookingResource extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;
}
