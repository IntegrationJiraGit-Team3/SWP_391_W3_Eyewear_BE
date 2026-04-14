package com.fpt.glasseshop.repository;

import com.fpt.glasseshop.entity.PreOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PreOrderRepository extends JpaRepository<PreOrder, Long> {
}
