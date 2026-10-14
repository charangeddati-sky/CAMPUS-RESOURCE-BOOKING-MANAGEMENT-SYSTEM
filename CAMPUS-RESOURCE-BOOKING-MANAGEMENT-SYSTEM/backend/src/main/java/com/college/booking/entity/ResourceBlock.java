package com.college.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "resource_blocks", indexes = {
        @Index(name = "idx_block_resource", columnList = "resource_id"),
        @Index(name = "idx_block_dates", columnList = "start_date,end_date")
})
public class ResourceBlock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private LocalTime startTime;
    private LocalTime endTime;

    @Column(nullable = false, length = 400)
    private String reason;

    @Column(nullable = false)
    private boolean active = true;
}
