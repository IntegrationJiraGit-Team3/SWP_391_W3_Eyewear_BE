package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.entity.dto.ApiResponse;
import com.fpt.glasseshop.entity.dto.response.RevenueResponse;
import com.fpt.glasseshop.service.ReportService;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping ("/api/admin/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueResponse>> getRevenueBetween(@RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) throws BadRequestException {
        RevenueResponse revenue = reportService.getRevenueBetween(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success("Revenue calculated successfully",revenue));
    }

    @GetMapping("/total")
    public ResponseEntity<ApiResponse<RevenueResponse>> getTotalRevenue() {
        RevenueResponse revenue = reportService.getOverallRevenue();
        return ResponseEntity.ok(ApiResponse.success("Overall revenue calculated successfully", revenue));
    }

}
