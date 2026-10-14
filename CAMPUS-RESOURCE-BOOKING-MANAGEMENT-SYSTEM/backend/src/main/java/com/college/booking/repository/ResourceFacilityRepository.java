package com.college.booking.repository;

import com.college.booking.entity.ResourceFacility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ResourceFacilityRepository extends JpaRepository<ResourceFacility, Long> {
    List<ResourceFacility> findByResourceId(Long resourceId);
    void deleteByResourceId(Long resourceId);

    @Query("""
            SELECT rf FROM ResourceFacility rf
            JOIN FETCH rf.facility
            JOIN FETCH rf.resource
            WHERE rf.resource.id IN :resourceIds
            """)
    List<ResourceFacility> findByResourceIdIn(@Param("resourceIds") Collection<Long> resourceIds);
}
