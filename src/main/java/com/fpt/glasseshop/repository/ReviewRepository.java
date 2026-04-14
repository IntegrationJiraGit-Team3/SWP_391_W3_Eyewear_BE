package com.fpt.glasseshop.repository;

import com.fpt.glasseshop.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductProductId(Long productId);
    List<Review> findByUserUserId(Long userId);
    List<Review> findByOrderOrderId(Long orderId);
    
    // Check if a user has already reviewed a specific product within a specific order
    boolean existsByUserUserIdAndProductProductIdAndOrderOrderId(Long userId, Long productId, Long orderId);
}
