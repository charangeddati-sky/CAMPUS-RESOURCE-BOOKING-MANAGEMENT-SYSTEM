package com.college.booking.controller;

import com.college.booking.entity.Building;
import com.college.booking.entity.Floor;
import com.college.booking.repository.BuildingRepository;
import com.college.booking.repository.FloorRepository;
import com.college.booking.repository.ResourceRepository;
import com.college.booking.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class BuildingFloorController {

    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final ResourceRepository resourceRepository;

    public BuildingFloorController(BuildingRepository buildingRepository,
                                   FloorRepository floorRepository,
                                   ResourceRepository resourceRepository) {
        this.buildingRepository = buildingRepository;
        this.floorRepository = floorRepository;
        this.resourceRepository = resourceRepository;
    }

    /* ─── Buildings ──────────────────────────────────────────── */

    @GetMapping("/buildings")
    public List<Building> listBuildings() {
        return buildingRepository.findAll();
    }

    @GetMapping("/buildings/{id}")
    public Building getBuilding(@PathVariable Long id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Building not found."));
    }

    @PostMapping("/buildings")
    @ResponseStatus(HttpStatus.CREATED)
    public Building createBuilding(@RequestBody Map<String, Object> body) {
        Building b = new Building();
        applyBuildingFields(b, body);
        return buildingRepository.save(b);
    }

    @PutMapping("/buildings/{id}")
    public Building updateBuilding(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Building b = buildingRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Building not found."));
        applyBuildingFields(b, body);
        return buildingRepository.save(b);
    }

    @DeleteMapping("/buildings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBuilding(@PathVariable Long id) {
        Building b = buildingRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Building not found."));
        List<Floor> floors = floorRepository.findByBuildingIdOrderByLevelAsc(b.getId());
        if (!floors.isEmpty()) {
            long totalResources = floors.stream()
                    .mapToLong(f -> resourceRepository.countByFloorIdAndEnabledTrue(f.getId()))
                    .sum();
            if (totalResources > 0) {
                throw ApiException.badRequest("BUILDING_HAS_RESOURCES",
                        "Cannot delete building with " + totalResources + " resource(s). Remove all resources first.");
            }
            floorRepository.deleteAll(floors);
        }
        buildingRepository.delete(b);
    }

    private void applyBuildingFields(Building b, Map<String, Object> body) {
        if (body.containsKey("name")) b.setName((String) body.get("name"));
        if (body.containsKey("code")) b.setCode((String) body.get("code"));
        if (body.containsKey("virtueName")) b.setVirtueName((String) body.get("virtueName"));
        if (body.containsKey("description")) b.setDescription((String) body.get("description"));
        if (body.containsKey("imageUrl")) b.setImageUrl((String) body.get("imageUrl"));
        if (body.containsKey("department")) b.setDepartment((String) body.get("department"));
        if (body.containsKey("bookable")) b.setBookable(Boolean.TRUE.equals(body.get("bookable")));
    }

    /* ─── Floors ─────────────────────────────────────────────── */

    @GetMapping("/buildings/{buildingId}/floors")
    public List<Floor> listFloors(@PathVariable Long buildingId) {
        return floorRepository.findByBuildingIdOrderByLevelAsc(buildingId);
    }

    @PostMapping("/buildings/{buildingId}/floors")
    @ResponseStatus(HttpStatus.CREATED)
    public Floor createFloor(@PathVariable Long buildingId, @RequestBody Map<String, Object> body) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> ApiException.notFound("Building not found."));
        Floor f = new Floor();
        f.setBuilding(building);
        applyFloorFields(f, body);
        return floorRepository.save(f);
    }

    @PutMapping("/floors/{id}")
    public Floor updateFloor(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Floor f = floorRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Floor not found."));
        applyFloorFields(f, body);
        return floorRepository.save(f);
    }

    @DeleteMapping("/floors/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFloor(@PathVariable Long id) {
        Floor f = floorRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Floor not found."));
        long resources = resourceRepository.countByFloorIdAndEnabledTrue(f.getId());
        if (resources > 0) {
            throw ApiException.badRequest("FLOOR_HAS_RESOURCES",
                    "Cannot delete floor with " + resources + " resource(s). Remove all resources first.");
        }
        floorRepository.delete(f);
    }

    private void applyFloorFields(Floor f, Map<String, Object> body) {
        if (body.containsKey("name")) f.setName((String) body.get("name"));
        if (body.containsKey("level")) f.setLevel(((Number) body.get("level")).intValue());
        if (body.containsKey("description")) f.setDescription((String) body.get("description"));
    }
}
