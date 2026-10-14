package com.college.booking.repository;

import com.college.booking.entity.Resource;
import com.college.booking.enums.ResourceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

    List<Resource> findByFloorIdAndEnabledTrue(Long floorId);

    List<Resource> findByBuildingIdAndEnabledTrue(Long buildingId);

    Optional<Resource> findByCode(String code);

    Optional<Resource> findByCodeIgnoreCase(String code);

    Optional<Resource> findByNameIgnoreCase(String name);

    List<Resource> findByEnabledTrueAndNameContainingIgnoreCase(String name);

    Optional<Resource> findByQrToken(String qrToken);

    @Query("""
            SELECT DISTINCT r FROM Resource r
            JOIN FETCH r.resourceType
            JOIN FETCH r.floor
            JOIN FETCH r.building
            WHERE r.enabled = true
            """)
    List<Resource> findAllEnabledDetailed();

    @Query("""
            SELECT DISTINCT r FROM Resource r
            JOIN FETCH r.resourceType
            JOIN FETCH r.floor
            JOIN FETCH r.building
            WHERE r.building.id = :buildingId AND r.enabled = true
            """)
    List<Resource> findEnabledDetailedByBuildingId(@Param("buildingId") Long buildingId);

    @Query("""
            SELECT DISTINCT r FROM Resource r
            JOIN FETCH r.resourceType
            JOIN FETCH r.floor
            JOIN FETCH r.building
            WHERE r.floor.id = :floorId AND r.enabled = true
            """)
    List<Resource> findEnabledDetailedByFloorId(@Param("floorId") Long floorId);

    long countByBuildingIdAndEnabledTrue(Long buildingId);

    long countByFloorIdAndEnabledTrue(Long floorId);

    long countByEnabledTrue();

    long countByOperationalStatusAndEnabledTrue(ResourceStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Resource r WHERE r.id = :id")
    Optional<Resource> lockById(@Param("id") Long id);

    @Query("""
            SELECT r FROM Resource r
            JOIN FETCH r.resourceType
            JOIN FETCH r.floor
            JOIN FETCH r.building
            WHERE r.id = :id
            """)
    Optional<Resource> findDetailedById(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT r FROM Resource r
            JOIN FETCH r.resourceType
            JOIN FETCH r.floor
            JOIN FETCH r.building
            WHERE r.enabled = true
              AND (:buildingId IS NULL OR r.building.id = :buildingId)
              AND (:floorId IS NULL OR r.floor.id = :floorId)
              AND (:typeCode IS NULL OR r.resourceType.code = :typeCode)
              AND (:department IS NULL OR LOWER(r.department) LIKE LOWER(CONCAT('%', :department, '%')))
              AND (:minCapacity IS NULL OR r.capacity >= :minCapacity)
              AND (
                    :q IS NULL OR :q = '' OR
                    LOWER(r.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
                    LOWER(r.code) LIKE LOWER(CONCAT('%', :q, '%')) OR
                    LOWER(r.department) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
            """)
    List<Resource> search(
            @Param("q") String q,
            @Param("buildingId") Long buildingId,
            @Param("floorId") Long floorId,
            @Param("typeCode") String typeCode,
            @Param("department") String department,
            @Param("minCapacity") Integer minCapacity
    );

    @Query("""
            SELECT DISTINCT r FROM Resource r
            JOIN FETCH r.resourceType
            JOIN FETCH r.floor
            JOIN FETCH r.building
            WHERE (:buildingId IS NULL OR r.building.id = :buildingId)
              AND (:floorId IS NULL OR r.floor.id = :floorId)
              AND (:typeCode IS NULL OR r.resourceType.code = :typeCode)
              AND (:enabled IS NULL OR r.enabled = :enabled)
              AND (
                    :q IS NULL OR :q = '' OR
                    LOWER(r.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
                    LOWER(r.code) LIKE LOWER(CONCAT('%', :q, '%')) OR
                    LOWER(r.department) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
            """)
    List<Resource> adminSearch(
            @Param("q") String q,
            @Param("buildingId") Long buildingId,
            @Param("floorId") Long floorId,
            @Param("typeCode") String typeCode,
            @Param("enabled") Boolean enabled
    );

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
