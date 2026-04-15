package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.dto.response.AdminDashboardSummaryResponse;
import com.fpt.glasseshop.entity.dto.response.RevenueResponse;
import org.apache.coyote.BadRequestException;

import java.time.LocalDate;

public interface ReportService {
    RevenueResponse getRevenueBetween(LocalDate fromDate, LocalDate toDate) throws BadRequestException;

    RevenueResponse getOverallRevenue();

    AdminDashboardSummaryResponse getDashboardSummary(LocalDate fromDate, LocalDate toDate) throws BadRequestException;
}