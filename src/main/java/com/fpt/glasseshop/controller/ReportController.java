package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.entity.dto.ApiResponse;
import com.fpt.glasseshop.entity.dto.response.AdminDashboardSummaryResponse;
import com.fpt.glasseshop.entity.dto.response.RevenueResponse;
import com.fpt.glasseshop.service.ReportService;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueResponse>> getRevenueBetween(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate
    ) throws BadRequestException {
        RevenueResponse revenue = reportService.getRevenueBetween(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success("Revenue calculated successfully", revenue));
    }

    @GetMapping("/total")
    public ResponseEntity<ApiResponse<RevenueResponse>> getTotalRevenue() {
        RevenueResponse revenue = reportService.getOverallRevenue();
        return ResponseEntity.ok(ApiResponse.success("Overall revenue calculated successfully", revenue));
    }

    @GetMapping("/dashboard-summary")
    public ResponseEntity<ApiResponse<AdminDashboardSummaryResponse>> getDashboardSummary(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate
    ) throws BadRequestException {
        AdminDashboardSummaryResponse summary = reportService.getDashboardSummary(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary loaded successfully", summary));
    }
}