package com.college.booking.repository;

import com.college.booking.entity.CampusEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampusEventRepository extends JpaRepository<CampusEvent, Long> {
    List<CampusEvent> findAllByOrderByEventDateDesc();
}
