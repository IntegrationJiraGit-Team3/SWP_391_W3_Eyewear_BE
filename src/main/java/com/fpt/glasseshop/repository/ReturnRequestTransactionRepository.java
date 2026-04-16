package com.fpt.glasseshop.repository;

import com.fpt.glasseshop.entity.ReturnRequestTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestTransactionRepository extends JpaRepository<ReturnRequestTransaction, Long> {
    List<ReturnRequestTransaction> findByReturnRequestRequestIdOrderByCreatedAtAsc(Long requestId);
}