package com.college.booking.repository;

import com.college.booking.entity.Maintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

    List<Maintenance> findByResourceIdAndActiveTrue(Long resourceId);

    @Query("""
            SELECT m FROM Maintenance m
            WHERE m.resource.id = :resourceId
              AND m.active = true
              AND m.startDate <= :date
              AND m.endDate >= :date
            """)
    List<Maintenance> findActiveOnDate(@Param("resourceId") Long resourceId, @Param("date") LocalDate date);

    List<Maintenance> findByActiveTrue();

    void deleteByResourceId(Long resourceId);

    @Query("""
            SELECT m.resource.id FROM Maintenance m
            WHERE m.active = true
              AND m.startDate <= :date
              AND m.endDate >= :date
            """)
    List<Long> findResourceIdsActiveOn(@Param("date") LocalDate date);

    @Query("""
            SELECT m.resource.id FROM Maintenance m
            WHERE m.active = true
              AND m.startDate <= :date
              AND m.endDate >= :date
              AND m.resource.id IN :resourceIds
            """)
    List<Long> findResourceIdsActiveOnIn(@Param("date") LocalDate date, @Param("resourceIds") Collection<Long> resourceIds);
}
