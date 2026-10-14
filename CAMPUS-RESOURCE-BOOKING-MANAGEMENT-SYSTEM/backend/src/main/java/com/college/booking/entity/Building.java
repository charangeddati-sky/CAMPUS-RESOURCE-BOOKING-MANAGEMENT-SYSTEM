package com.college.booking.entity;

import com.college.booking.enums.BuildingKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "buildings", indexes = {
        @Index(name = "idx_buildings_code", columnList = "code", unique = true)
})
public class Building extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(length = 80)
    private String virtueName;

    @Column(length = 2000)
    private String description;

    @Column(length = 400)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BuildingKind kind = BuildingKind.ACADEMIC;

    @Column(nullable = false)
    private boolean bookable = true;

    /** Percentage position on the campus aerial photo (0-100). */
    private Double mapX;
    private Double mapY;
    private Double mapWidth;
    private Double mapHeight;

    /** Percentage position on the schematic architecture diagram (0-100). */
    private Double schematicX;
    private Double schematicY;
    private Double schematicWidth;
    private Double schematicHeight;

    @Column(length = 80)
    private String department;
}
