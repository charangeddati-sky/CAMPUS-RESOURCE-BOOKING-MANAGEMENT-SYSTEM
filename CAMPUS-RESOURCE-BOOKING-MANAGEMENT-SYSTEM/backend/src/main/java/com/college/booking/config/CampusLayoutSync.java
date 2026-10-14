package com.college.booking.config;

import com.college.booking.repository.BuildingRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Always re-applies aerial (blue-box) and schematic coordinates so map
 * corrections land on existing databases, not only on first seed.
 */
@Component
@Profile("!test")
@Order(30)
public class CampusLayoutSync implements CommandLineRunner {

    private final BuildingRepository buildingRepository;

    public CampusLayoutSync(BuildingRepository buildingRepository) {
        this.buildingRepository = buildingRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        apply("GROUND", 26.2, 31.2, 15.8, 18.2, 2.5, 4.0, 24.0, 50.0);
        apply("LOYALTY2", 40.0, 34.6, 10.2, 10.8, 29.5, 2.5, 20.5, 22.0);
        apply("WISDOM", 47.0, 41.2, 8.0, 11.8, 51.5, 24.0, 18.5, 26.5);
        apply("LOYALTY3", 52.8, 32.2, 13.2, 14.0, 73.0, 2.5, 23.0, 24.0);
        apply("LOYALTY1", 38.8, 45.2, 10.4, 12.2, 29.5, 33.0, 20.5, 28.0);
        apply("LOYALTY4", 53.6, 44.8, 11.8, 13.2, 73.0, 35.5, 23.0, 24.5);
        apply("TRUTH", 14.2, 55.5, 18.0, 31.0, 7.5, 61.0, 14.0, 35.0);
        apply("HONESTY", 36.2, 54.8, 41.0, 24.5, 28.0, 65.5, 68.0, 29.0);
    }

    private void apply(String code, double x, double y, double w, double h,
                       double sx, double sy, double sw, double sh) {
        buildingRepository.findByCode(code).ifPresent(b -> {
            b.setMapX(x);
            b.setMapY(y);
            b.setMapWidth(w);
            b.setMapHeight(h);
            b.setSchematicX(sx);
            b.setSchematicY(sy);
            b.setSchematicWidth(sw);
            b.setSchematicHeight(sh);
            buildingRepository.save(b);
        });
    }
}
