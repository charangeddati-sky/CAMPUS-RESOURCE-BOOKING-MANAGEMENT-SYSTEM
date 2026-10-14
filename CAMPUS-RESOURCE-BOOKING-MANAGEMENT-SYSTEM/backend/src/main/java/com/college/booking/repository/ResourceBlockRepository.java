package com.college.booking.repository;

import com.college.booking.entity.ResourceBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ResourceBlockRepository extends JpaRepository<ResourceBlock, Long> {

    List<ResourceBlock> findByResourceIdAndActiveTrue(Long resourceId);

    @Query("""
            SELECT b FROM ResourceBlock b
            WHERE b.resource.id = :resourceId
              AND b.active = true
              AND b.startDate <= :date
              AND b.endDate >= :date
            """)
    List<ResourceBlock> findActiveOnDate(@Param("resourceId") Long resourceId, @Param("date") LocalDate date);

    @Query("""
            SELECT b FROM ResourceBlock b
            JOIN FETCH b.resource
            WHERE b.active = true
              AND b.startDate <= :date
              AND b.endDate >= :date
            """)
    List<ResourceBlock> findActiveOn(@Param("date") LocalDate date);

    @Query("""
            SELECT b FROM ResourceBlock b
            JOIN FETCH b.resource
            WHERE b.active = true
              AND b.startDate <= :date
              AND b.endDate >= :date
              AND b.resource.id IN :resourceIds
            """)
    List<ResourceBlock> findActiveOnIn(@Param("date") LocalDate date, @Param("resourceIds") Collection<Long> resourceIds);

    void deleteByResourceId(Long resourceId);
}
