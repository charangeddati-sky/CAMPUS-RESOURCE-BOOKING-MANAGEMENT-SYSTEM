package com.college.booking.repository;

import com.college.booking.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Favorite> findByUserIdAndResourceId(Long userId, Long resourceId);
    boolean existsByUserIdAndResourceId(Long userId, Long resourceId);
    void deleteByUserIdAndResourceId(Long userId, Long resourceId);
    void deleteByResourceId(Long resourceId);

    @Query("SELECT f.resource.id FROM Favorite f WHERE f.user.id = :userId")
    Set<Long> findResourceIdsByUserId(@Param("userId") Long userId);
}
