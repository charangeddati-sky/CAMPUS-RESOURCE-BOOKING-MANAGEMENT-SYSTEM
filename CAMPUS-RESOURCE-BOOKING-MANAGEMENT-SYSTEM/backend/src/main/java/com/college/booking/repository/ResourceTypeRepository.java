package com.college.booking.repository;

import com.college.booking.entity.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResourceTypeRepository extends JpaRepository<ResourceType, Long> {
    Optional<ResourceType> findByCode(String code);
}
