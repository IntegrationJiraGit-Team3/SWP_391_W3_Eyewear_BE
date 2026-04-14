package com.fpt.glasseshop.repository;

import com.fpt.glasseshop.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserUserIdAndIsReadFalse(Long userId);
    void deleteByUserUserId(Long userId);
}
