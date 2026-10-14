package com.college.booking.repository;

import com.college.booking.entity.Floor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FloorRepository extends JpaRepository<Floor, Long> {
    List<Floor> findByBuildingIdOrderByLevelAsc(Long buildingId);
    long countByBuildingId(Long buildingId);

    @Query("SELECT f.building.id, COUNT(f) FROM Floor f GROUP BY f.building.id")
    List<Object[]> countGroupedByBuilding();

    @Query("SELECT DISTINCT f FROM Floor f JOIN FETCH f.building ORDER BY f.building.id, f.level")
    List<Floor> findAllWithBuilding();
}
