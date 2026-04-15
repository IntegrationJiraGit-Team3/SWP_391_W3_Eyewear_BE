package com.fpt.glasseshop.repository;

import com.fpt.glasseshop.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    boolean existsByOrderItemOrderItemId(Long orderId);

    Optional<ReturnRequest> findByOrderItemOrderItemId(Long orderItemId);

    List<ReturnRequest> findAllByOrderItemOrderItemId(Long orderItemId);

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

    @Query("""
        select count(r)
        from ReturnRequest r
        where r.status = :status
    """)
    Long countByReturnStatus(@Param("status") ReturnRequest.ReturnStatus status);

    @Query("""
        select coalesce(sum((coalesce(oi.unitPrice, 0) + coalesce(oi.lensPrice, 0)) * coalesce(r.returnQuantity, 0)), 0)
        from ReturnRequest r
        join r.orderItem oi
        where r.status = :status
    """)
    BigDecimal sumRefundAmountByStatus(@Param("status") ReturnRequest.ReturnStatus status);

    @Query("""
        select coalesce(sum((coalesce(oi.unitPrice, 0) + coalesce(oi.lensPrice, 0)) * coalesce(r.returnQuantity, 0)), 0)
        from ReturnRequest r
        join r.orderItem oi
        where r.status in :statuses
    """)
    BigDecimal sumRefundAmountByStatuses(@Param("statuses") List<ReturnRequest.ReturnStatus> statuses);

    @Query("""
        select coalesce(sum((coalesce(oi.unitPrice, 0) + coalesce(oi.lensPrice, 0)) * coalesce(r.returnQuantity, 0)), 0)
        from ReturnRequest r
        join r.orderItem oi
        where r.status in :statuses
          and r.requestedAt >= :from
          and r.requestedAt <= :to
    """)
    BigDecimal sumRefundAmountBetween(
            @Param("statuses") List<ReturnRequest.ReturnStatus> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}