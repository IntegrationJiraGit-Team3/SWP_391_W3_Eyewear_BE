package com.fpt.glasseshop.repository;

import com.fpt.glasseshop.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    boolean existsByOrderItemOrderItemId(Long orderId);
    Optional<ReturnRequest> findByOrderItemOrderItemId(Long orderItemId);
    @Query("""
    select coalesce(sum(r.returnQuantity), 0)
    from ReturnRequest r
    where r.orderItem.orderItemId = :orderItemId
      and r.status <> :excludedStatus
""")
    Integer sumRequestedQuantityByOrderItemId(
            @Param("orderItemId") Long orderItemId,
            @Param("excludedStatus") ReturnRequest.ReturnStatus excludedStatus
    );

    List<ReturnRequest> findAllByOrderItemOrderItemId(Long orderItemId);
}
