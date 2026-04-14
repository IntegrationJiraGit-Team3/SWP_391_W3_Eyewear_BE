package com.fpt.glasseshop.repository;

import com.fpt.glasseshop.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserUserId(Long userId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Cart c WHERE c.user.userId = :userId")
    Optional<Cart> findByUserUserIdForCheckout(@org.springframework.data.repository.query.Param("userId") Long userId);
}
