package com.college.booking.repository;

import com.college.booking.entity.RecentlyVisited;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecentlyVisitedRepository extends JpaRepository<RecentlyVisited, Long> {
    List<RecentlyVisited> findTop8ByUserIdOrderByVisitedAtDesc(Long userId);
    Optional<RecentlyVisited> findByUserIdAndResourceId(Long userId, Long resourceId);
    void deleteByResourceId(Long resourceId);
}
