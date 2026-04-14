package com.fpt.glasseshop.repository;

import com.fpt.glasseshop.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(String name, String brand);
}
