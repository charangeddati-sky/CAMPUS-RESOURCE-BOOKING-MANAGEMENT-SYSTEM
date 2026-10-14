package com.college.booking.entity;

import com.college.booking.enums.ResourceStatus;
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

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "resources", indexes = {
        @Index(name = "idx_resources_floor", columnList = "floor_id"),
        @Index(name = "idx_resources_building", columnList = "building_id"),
        @Index(name = "idx_resources_type", columnList = "resource_type_id"),
        @Index(name = "idx_resources_status", columnList = "operational_status"),
        @Index(name = "idx_resources_code", columnList = "code", unique = true)
})
public class Resource extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_type_id", nullable = false)
    private ResourceType resourceType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(nullable = false)
    private Integer capacity = 0;

    @Column(length = 80)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false, length = 30)
    private ResourceStatus operationalStatus = ResourceStatus.AVAILABLE;

    @Column(length = 2000)
    private String description;

    @Column(length = 400)
    private String imageUrl;

    /** Floor-map layout stored as percentage of the canvas. */
    private Double positionX = 0d;
    private Double positionY = 0d;
    private Double width = 12d;
    private Double height = 14d;
    private Double rotation = 0d;

    private LocalTime workingHoursStart = LocalTime.of(8, 0);
    private LocalTime workingHoursEnd = LocalTime.of(18, 0);

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(nullable = false, unique = true, length = 64)
    private String qrToken;

    private Boolean projector;
    private Boolean smartBoard;
    private Boolean airConditioned;
    private Boolean wifi;
    private Boolean audio;
    private Boolean microphones;
    private Boolean stage;
    private Integer computers;
    private Integer studySeats;
    private Boolean readingArea;
    @Column(length = 80)
    private String openingHours;
    @Column(length = 400)
    private String equipmentNotes;
    @Column(length = 400)
    private String softwareNotes;
    @Column(length = 80)
    private String sportsType;
    @Column(length = 80)
    private String seatingArrangement;

    @Column(nullable = false)
    private boolean enabled = true;
}
