package com.college.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "equipment")
public class Equipment extends BaseEntity {

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(nullable = false)
    private Integer available = 1;

    @Column(length = 400)
    private String description;

    @Column(nullable = false)
    private boolean enabled = true;
}
