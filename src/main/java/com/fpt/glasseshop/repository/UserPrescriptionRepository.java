package com.fpt.glasseshop.repository;

import com.fpt.glasseshop.entity.UserPrescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPrescriptionRepository extends JpaRepository<UserPrescription, Long> {
    List<UserPrescription> findByUserUserId(Long userId);
}