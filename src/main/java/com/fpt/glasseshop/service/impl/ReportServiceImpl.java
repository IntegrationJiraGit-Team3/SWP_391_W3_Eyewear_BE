package com.fpt.glasseshop.service.impl;

import com.fpt.glasseshop.entity.ReturnRequest;
import com.fpt.glasseshop.entity.dto.response.AdminDashboardSummaryResponse;
import com.fpt.glasseshop.entity.dto.response.RevenueResponse;
import com.fpt.glasseshop.repository.OrderRepository;
import com.fpt.glasseshop.repository.ReturnRequestRepository;
import com.fpt.glasseshop.service.ReportService;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Override
    public RevenueResponse getRevenueBetween(LocalDate fromDate, LocalDate toDate) throws BadRequestException {
        if (fromDate == null || toDate == null) {
            throw new BadRequestException("fromDate and toDate are required");
        }

        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be before or equal to toDate");
        }

        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(23, 59, 59);

        BigDecimal totalRevenue = orderRepo.calculateRevenueBetween(from, to);
        Long totalOrders = orderRepo.countDeliveredOrdersBetween(from, to);

        return RevenueResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public RevenueResponse getOverallRevenue() {
        BigDecimal totalRevenue = orderRepo.calculateTotalRevenue();
        Long totalOrders = orderRepo.countDeliveredOrders();

        return RevenueResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public AdminDashboardSummaryResponse getDashboardSummary(LocalDate fromDate, LocalDate toDate) throws BadRequestException {
        if (fromDate == null || toDate == null) {
            throw new BadRequestException("fromDate and toDate are required");
        }

        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be before or equal to toDate");
        }

        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(23, 59, 59);

        BigDecimal grossRevenue = orderRepo.calculateGrossRevenueBetween(from, to);
        BigDecimal refundedAmount = returnRequestRepository.sumRefundAmountBetween(
                List.of(ReturnRequest.ReturnStatus.REFUND_PENDING, ReturnRequest.ReturnStatus.REFUNDED),
                from,
                to
        );
        BigDecimal netRevenue = grossRevenue.subtract(refundedAmount);

        BigDecimal collectedCash = orderRepo.calculateCollectedCash();
        BigDecimal currentHeldMoney = orderRepo.calculateCurrentHeldMoney();

        Long totalOrders = orderRepo.count();
        Long deliveredOrders = orderRepo.countDeliveredOrders();
        Long pendingOrders = orderRepo.countByOrderStatus("PENDING");
        Long processingOrders = orderRepo.countByOrderStatus("PROCESSING");
        Long shippedOrders = orderRepo.countByOrderStatus("SHIPPED");
        Long deliveringOrders = orderRepo.countByOrderStatus("DELIVERING");
        Long shippingOrders = (shippedOrders == null ? 0L : shippedOrders) + (deliveringOrders == null ? 0L : deliveringOrders);
        Long cancelledOrders = (orderRepo.countByOrderStatus("CANCELED") == null ? 0L : orderRepo.countByOrderStatus("CANCELED"))
                + (orderRepo.countByOrderStatus("CANCELLED") == null ? 0L : orderRepo.countByOrderStatus("CANCELLED"));

        Long refundPendingCount = returnRequestRepository.countByReturnStatus(ReturnRequest.ReturnStatus.REFUND_PENDING);
        Long refundedCount = returnRequestRepository.countByReturnStatus(ReturnRequest.ReturnStatus.REFUNDED);

        return AdminDashboardSummaryResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .grossRevenue(grossRevenue)
                .refundedAmount(refundedAmount)
                .netRevenue(netRevenue)
                .collectedCash(collectedCash)
                .currentHeldMoney(currentHeldMoney)
                .totalOrders(totalOrders)
                .deliveredOrders(deliveredOrders)
                .pendingOrders(pendingOrders)
                .processingOrders(processingOrders)
                .shippingOrders(shippingOrders)
                .cancelledOrders(cancelledOrders)
                .refundPendingCount(refundPendingCount)
                .refundedCount(refundedCount)
                .timestamp(LocalDateTime.now())
                .build();
    }
}