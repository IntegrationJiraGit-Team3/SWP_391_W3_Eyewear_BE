package com.fpt.glasseshop.repository;

import com.fpt.glasseshop.entity.LensOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LensOptionRepository extends JpaRepository<LensOption, Long> {
}
