package com.fpt.glasseshop.service.impl;

import com.fpt.glasseshop.entity.dto.response.RevenueResponse;
import com.fpt.glasseshop.repository.OrderRepository;
import com.fpt.glasseshop.service.ReportService;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderRepository orderRepo;

    @Override
    public RevenueResponse getRevenueBetween(LocalDate fromDate, LocalDate toDate) throws BadRequestException {

        if (fromDate == null || toDate == null) {
            throw new BadRequestException("fromDate and toDate are required");
        }

        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be before or equal to toDate");
        }

        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(23,59,59);

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
}
