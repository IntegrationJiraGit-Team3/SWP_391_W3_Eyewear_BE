package com.fpt.glasseshop.service.impl;

import com.fpt.glasseshop.entity.Order;
import com.fpt.glasseshop.entity.OrderItem;
import com.fpt.glasseshop.entity.Product;
import com.fpt.glasseshop.entity.ProductVariant;
import com.fpt.glasseshop.entity.ReturnRequest;
import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.entity.dto.response.AdminDashboardAnalyticsResponse;
import com.fpt.glasseshop.entity.dto.response.AdminDashboardSummaryResponse;
import com.fpt.glasseshop.entity.dto.response.CustomerInsightResponse;
import com.fpt.glasseshop.entity.dto.response.CustomerPurchaseSummaryResponse;
import com.fpt.glasseshop.entity.dto.response.DashboardTimePointResponse;
import com.fpt.glasseshop.entity.dto.response.OrderInsightResponse;
import com.fpt.glasseshop.entity.dto.response.OrderStatusReportResponse;
import com.fpt.glasseshop.entity.dto.response.ProductInventoryReportResponse;
import com.fpt.glasseshop.entity.dto.response.ProductReportResponse;
import com.fpt.glasseshop.entity.dto.response.LensReportResponse;
import com.fpt.glasseshop.entity.dto.response.ReturnStatusReportResponse;
import com.fpt.glasseshop.entity.dto.response.RevenueResponse;
import com.fpt.glasseshop.repository.OrderRepository;
import com.fpt.glasseshop.repository.PaymentRepository;
import com.fpt.glasseshop.repository.ProductVariantRepository;
import com.fpt.glasseshop.repository.ReturnRequestRepository;
import com.fpt.glasseshop.repository.UserAccountRepository;
import com.fpt.glasseshop.service.ReportService;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private static final int TOP_CUSTOMERS_LIMIT = 10;
    private static final int TOP_ORDERS_LIMIT = 10;

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

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

        BigDecimal grossRevenue = safeAmount(orderRepo.calculateGrossRevenueIncludingVnpayPaidBetween(from, to));
        BigDecimal refundedAmount = safeAmount(returnRequestRepository.sumRefundAmountBetween(
                List.of(ReturnRequest.ReturnStatus.REFUND_PENDING, ReturnRequest.ReturnStatus.REFUNDED),
                from,
                to
        ));

        BigDecimal refundedFromCancelledVnpayOrders = safeAmount(paymentRepository.sumVnpayRefundedAmountsBetween(from, to)).abs();
        refundedAmount = refundedAmount.add(refundedFromCancelledVnpayOrders);

        BigDecimal netRevenue = grossRevenue.subtract(refundedAmount);

        BigDecimal collectedCash = safeAmount(orderRepo.calculateCollectedCash());
        BigDecimal currentHeldMoney = safeAmount(orderRepo.calculateCurrentHeldMoney());

        Long totalOrders = orderRepo.count();
        Long deliveredOrders = orderRepo.countDeliveredOrders();
        Long pendingOrders = orderRepo.countByOrderStatus("PENDING");
        Long processingOrders = orderRepo.countByOrderStatus("PROCESSING");
        Long shippedOrders = orderRepo.countByOrderStatus("SHIPPED");
        Long deliveringOrders = orderRepo.countByOrderStatus("DELIVERING");
        Long shippingOrders = safeLong(shippedOrders) + safeLong(deliveringOrders);
        Long cancelledOrders = safeLong(orderRepo.countByOrderStatus("CANCELED"))
                + safeLong(orderRepo.countByOrderStatus("CANCELLED"));

        Long refundPendingCount = returnRequestRepository.countByReturnStatus(ReturnRequest.ReturnStatus.REFUND_PENDING);
        Long refundedCount = returnRequestRepository.countByReturnStatus(ReturnRequest.ReturnStatus.REFUNDED);

        return AdminDashboardSummaryResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .grossRevenue(grossRevenue)
                .refundedAmount(refundedAmount)
                .netRevenue(netRevenue)
                .remainingRevenueAfterRefund(netRevenue)
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

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardAnalyticsResponse getDashboardAnalytics(LocalDate fromDate, LocalDate toDate, String groupBy) throws BadRequestException {
        validateDateRange(fromDate, toDate);

        String normalizedGroupBy = normalizeGroupBy(groupBy);

        List<Order> allOrders = orderRepo.findAll();
        List<UserAccount> allCustomers = userAccountRepository.findByRoleIgnoreCase("CUSTOMER");
        List<ReturnRequest> allReturns = returnRequestRepository.findAll();

        LocalDate previousToDate = fromDate.minusDays(1);
        long dayLength = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        LocalDate previousFromDate = fromDate.minusDays(dayLength);

        List<Order> currentOrdersInRange = allOrders.stream()
                .filter(order -> isWithinRange(getSafeOrderDate(order), fromDate, toDate))
                .toList();

        Set<Long> refundedOrderIds = allReturns.stream()
                .filter(req -> req != null && req.getRequestType() == ReturnRequest.RequestType.RETURN)
                .filter(this::isActuallyRefunded)
                .map(req -> {
                    if (req.getOrderItem() == null || req.getOrderItem().getOrder() == null) {
                        return null;
                    }
                    return req.getOrderItem().getOrder().getOrderId();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Order> previousOrdersInRange = allOrders.stream()
                .filter(order -> isWithinRange(getSafeOrderDate(order), previousFromDate, previousToDate))
                .toList();

        List<Order> currentCompletedOrders = currentOrdersInRange.stream()
            .filter(this::isCompletedOrder)
            .toList();

        List<Order> currentRevenueRecognizedOrders = currentOrdersInRange.stream()
            .filter(order -> isCompletedOrder(order) || isVnpayPaidOrder(order))
            .toList();

        List<Order> previousCompletedOrders = previousOrdersInRange.stream()
            .filter(this::isCompletedOrder)
            .toList();

        List<Order> previousRevenueRecognizedOrders = previousOrdersInRange.stream()
            .filter(order -> isCompletedOrder(order) || isVnpayPaidOrder(order))
            .toList();

        BigDecimal grossRevenue = sumOrderRevenue(currentRevenueRecognizedOrders);
        BigDecimal previousRevenue = sumOrderRevenue(previousRevenueRecognizedOrders);

        BigDecimal codRevenue = currentRevenueRecognizedOrders.stream()
            .filter(order -> order != null && order.getPaymentMethod() != null)
            .filter(order -> "COD".equalsIgnoreCase(order.getPaymentMethod().trim()))
            .map(this::getOrderAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal vnpayRevenue = currentRevenueRecognizedOrders.stream()
            .filter(order -> order != null && order.getPaymentMethod() != null)
            .filter(order -> "VNPAY".equalsIgnoreCase(order.getPaymentMethod().trim()))
            .map(this::getOrderAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long soldItems = sumSoldItems(currentRevenueRecognizedOrders);
        long previousSoldItems = sumSoldItems(previousRevenueRecognizedOrders);

        BigDecimal pendingRevenue = currentOrdersInRange.stream()
                .filter(this::isProjectedOrder)
            .filter(order -> !isVnpayPaidOrder(order))
                .map(this::getOrderAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalCustomers = allCustomers.size();

        List<UserAccount> currentNewCustomers = allCustomers.stream()
                .filter(user -> isWithinRange(getSafeUserCreatedDate(user), fromDate, toDate))
                .toList();

        List<UserAccount> previousNewCustomers = allCustomers.stream()
                .filter(user -> isWithinRange(getSafeUserCreatedDate(user), previousFromDate, previousToDate))
                .toList();

        long newCustomers = currentNewCustomers.size();
        long previousNewCustomersCount = previousNewCustomers.size();

        List<ReturnRequest> currentReturnRequests = allReturns.stream()
                .filter(request -> isWithinRange(getSafeReturnTrackingDate(request), fromDate, toDate))
                .toList();

        List<ReturnRequest> currentRefundedReturns = currentReturnRequests.stream()
                .filter(this::isActuallyRefunded)
                .toList();

        List<ReturnRequest> previousRefundedReturns = allReturns.stream()
                .filter(this::isActuallyRefunded)
                .filter(request -> isWithinRange(getSafeRefundDate(request), previousFromDate, previousToDate))
                .toList();

        BigDecimal refundedAmount = sumRefundAmount(currentRefundedReturns);
        BigDecimal previousRefundedAmount = sumRefundAmount(previousRefundedReturns);

        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(23, 59, 59);
        LocalDateTime previousFrom = previousFromDate.atStartOfDay();
        LocalDateTime previousTo = previousToDate.atTime(23, 59, 59);

        BigDecimal refundedFromCancelledVnpayOrders = safeAmount(paymentRepository.sumVnpayRefundedAmountsBetween(from, to)).abs();
        BigDecimal previousRefundedFromCancelledVnpayOrders = safeAmount(paymentRepository.sumVnpayRefundedAmountsBetween(previousFrom, previousTo)).abs();

        refundedAmount = refundedAmount.add(refundedFromCancelledVnpayOrders);
        previousRefundedAmount = previousRefundedAmount.add(previousRefundedFromCancelledVnpayOrders);

        BigDecimal netRevenue = grossRevenue.subtract(refundedAmount);

        long pendingOrders = 0;
        long processingOrders = 0;
        long shippingOrders = 0;
        long cancelledOrders = 0;
        long completedOrders = 0;
        long refundedOrders = 0;

        for (Order order : currentOrdersInRange) {
            if (order == null) {
                continue;
            }

            if (isCancelledOrder(order)) {
                cancelledOrders++;
                continue;
            }

            boolean isRefunded = refundedOrderIds.contains(order.getOrderId());
            if (!isRefunded && order.getRefundStatus() != null) {
                isRefunded = "REFUNDED".equalsIgnoreCase(order.getRefundStatus().trim());
            }

            if (isRefunded) {
                refundedOrders++;
                continue;
            }

            if (isCompletedOrder(order)) {
                completedOrders++;
                continue;
            }

            if (isShippingOrder(order)) {
                shippingOrders++;
                continue;
            }

            if (isProcessingOrder(order)) {
                processingOrders++;
                continue;
            }

            // Default bucket: Pending / Preorder / unknown
            pendingOrders++;
        }

        long totalOrders = currentOrdersInRange.size();
        long processedOrders = completedOrders + refundedOrders + cancelledOrders;

        long refundPendingCount = currentReturnRequests.stream().filter(this::isRefundPending).count();
        long refundedCount = currentRefundedReturns.size();
        long pendingReturnRequests = currentReturnRequests.stream().filter(this::isPendingReturnRequest).count();
        long totalReturnRequests = currentReturnRequests.size();

        double refundRate = percentage(refundedOrders, processedOrders);
        double completionRate = percentage(completedOrders, processedOrders);

        BigDecimal totalFrameRevenue = buildTotalFrameRevenue(currentOrdersInRange, currentReturnRequests);
        BigDecimal totalLensRevenue = buildTotalLensRevenue(currentOrdersInRange, currentReturnRequests);

        return AdminDashboardAnalyticsResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .groupBy(normalizedGroupBy)
                .totalRevenue(grossRevenue)
                .grossRevenue(grossRevenue)
                .netRevenue(netRevenue)
                .pendingRevenue(pendingRevenue)
                .projectedRevenue(pendingRevenue)
                .refundedAmount(refundedAmount)
                .remainingRevenueAfterRefund(netRevenue)
                .totalFrameRevenue(totalFrameRevenue)
                .totalLensRevenue(totalLensRevenue)
                .totalProductRevenue(totalFrameRevenue.add(totalLensRevenue))
            .codRevenue(codRevenue)
            .vnpayRevenue(vnpayRevenue)
                .totalCustomers(totalCustomers)
                .newCustomers(newCustomers)
                .soldItems(soldItems)
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .pendingOrders(pendingOrders)
                .processingOrders(processingOrders)
                .shippingOrders(shippingOrders)
                .cancelledOrders(cancelledOrders)
                .refundedOrders(refundedOrders)
                .processedOrders(processedOrders)
                .totalReturnRequests(totalReturnRequests)
                .pendingReturnRequests(pendingReturnRequests)
                .refundPendingCount(refundPendingCount)
                .refundedCount(refundedCount)
                .revenueChangePercent(calculateChangePercent(grossRevenue, previousRevenue))
                .customerChangePercent(calculateChangePercent(BigDecimal.valueOf(newCustomers), BigDecimal.valueOf(previousNewCustomersCount)))
                .soldItemsChangePercent(calculateChangePercent(BigDecimal.valueOf(soldItems), BigDecimal.valueOf(previousSoldItems)))
                .refundedChangePercent(calculateChangePercent(refundedAmount, previousRefundedAmount))
                .refundRate(refundRate)
                .completionRate(completionRate)
                .orderStatusReport(buildOrderStatusReport(
                        pendingOrders,
                        processingOrders,
                        shippingOrders,
                        completedOrders,
                        cancelledOrders
                ))
                .returnStatusReport(buildReturnStatusReport(currentReturnRequests))
                .timeline(buildTimeline(
                        fromDate,
                        toDate,
                        normalizedGroupBy,
                    currentRevenueRecognizedOrders,
                        currentNewCustomers,
                        currentRefundedReturns
                ))
                .bestSellingProductsByQuantity(buildTopProductsByQuantity(currentCompletedOrders))
                .bestSellingProductsByRevenue(buildTopProductsByRevenue(currentOrdersInRange, currentReturnRequests))
                .bestSellingFramesByQuantity(buildTopFramesByQuantity(currentCompletedOrders))
                .bestSellingFramesByRevenue(buildTopFramesByRevenue(currentOrdersInRange, currentReturnRequests))
                .bestSellingLensesByQuantity(buildTopLensesByQuantity(currentCompletedOrders))
                .bestSellingLensesByRevenue(buildTopLensesByRevenue(currentOrdersInRange, currentReturnRequests))
                .lensSalesReport(buildLensSalesReport(currentOrdersInRange, currentReturnRequests))
                .productInventoryReport(buildProductInventoryReport(currentOrdersInRange, currentReturnRequests))
                .topCustomersBySpending(buildTopCustomersBySpending(currentCompletedOrders))
                .customerInsights(buildCustomerInsights(currentOrdersInRange))
                .topOrders(buildTopOrders(currentOrdersInRange))
                .cancelledOrdersReport(buildCancelledOrdersReport(currentOrdersInRange))
                .build();
    }

    private BigDecimal buildTotalFrameRevenue(List<Order> ordersInRange, List<ReturnRequest> returnRequests) {
        BigDecimal total = BigDecimal.ZERO;

        if (ordersInRange != null) {
            for (Order order : ordersInRange) {
                for (OrderItem item : getSafeOrderItems(order)) {
                    long quantity = item != null && item.getQuantity() != null ? item.getQuantity() : 0L;
                    total = total.add(calculateFrameRevenue(item, quantity));
                }
            }
        }

        if (returnRequests != null) {
            for (ReturnRequest request : returnRequests) {
                if (request == null || request.getOrderItem() == null) {
                    continue;
                }
                if (request.getStatus() != ReturnRequest.ReturnStatus.REFUND_PENDING
                        && request.getStatus() != ReturnRequest.ReturnStatus.REFUNDED
                        && request.getStatus() != ReturnRequest.ReturnStatus.COMPLETED) {
                    continue;
                }

                OrderItem item = request.getOrderItem();
                long quantity = request.getReturnQuantity() != null ? request.getReturnQuantity() : 0L;
                total = total.add(calculateFrameRevenue(item, quantity));
            }
        }

        return total;
    }

    private BigDecimal buildTotalLensRevenue(List<Order> ordersInRange, List<ReturnRequest> returnRequests) {
        BigDecimal total = BigDecimal.ZERO;

        if (ordersInRange != null) {
            for (Order order : ordersInRange) {
                for (OrderItem item : getSafeOrderItems(order)) {
                    if (Boolean.TRUE.equals(item != null ? item.getIsPreorder() : null)) {
                        continue;
                    }
                    long quantity = item != null && item.getQuantity() != null ? item.getQuantity() : 0L;
                    total = total.add(calculateLensRevenue(item, quantity));
                }
            }
        }

        if (returnRequests != null) {
            for (ReturnRequest request : returnRequests) {
                if (request == null || request.getOrderItem() == null) {
                    continue;
                }
                if (request.getStatus() != ReturnRequest.ReturnStatus.REFUND_PENDING
                        && request.getStatus() != ReturnRequest.ReturnStatus.REFUNDED
                        && request.getStatus() != ReturnRequest.ReturnStatus.COMPLETED) {
                    continue;
                }

                OrderItem item = request.getOrderItem();
                if (Boolean.TRUE.equals(item != null ? item.getIsPreorder() : null)) {
                    continue;
                }
                long quantity = request.getReturnQuantity() != null ? request.getReturnQuantity() : 0L;
                total = total.add(calculateLensRevenue(item, quantity));
            }
        }

        return total;
    }

    private List<OrderInsightResponse> buildTopOrders(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }

        return orders.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(this::getOrderAmount, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .limit(TOP_ORDERS_LIMIT)
                .map(this::toOrderInsight)
                .toList();
    }

    private List<OrderInsightResponse> buildCancelledOrdersReport(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }

        return orders.stream()
                .filter(Objects::nonNull)
                .filter(this::isCancelledOrder)
                .sorted(Comparator.comparing(this::getOrderAmount, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .limit(TOP_ORDERS_LIMIT)
                .map(this::toOrderInsight)
                .toList();
    }

    private OrderInsightResponse toOrderInsight(Order order) {
        if (order == null) {
            return null;
        }

        String customerName = null;
        if (order.getFullName() != null && !order.getFullName().isBlank()) {
            customerName = order.getFullName().trim();
        } else if (order.getUser() != null && order.getUser().getName() != null && !order.getUser().getName().isBlank()) {
            customerName = order.getUser().getName().trim();
        } else {
            customerName = "Guest";
        }

        long itemCount = 0;
        if (order.getOrderItems() != null) {
            itemCount = order.getOrderItems().stream()
                    .filter(Objects::nonNull)
                    .mapToLong(item -> item.getQuantity() == null ? 0 : Math.max(item.getQuantity(), 0))
                    .sum();
        }

        return OrderInsightResponse.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .customerName(customerName)
                .status(order.getStatus())
                .totalAmount(getOrderAmount(order))
                .itemCount(itemCount)
                .orderDate(order.getOrderDate())
                .build();
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) throws BadRequestException {
        if (fromDate == null || toDate == null) {
            throw new BadRequestException("fromDate and toDate are required");
        }

        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be before or equal to toDate");
        }
    }

    private String normalizeGroupBy(String groupBy) {
        if (groupBy == null || groupBy.isBlank()) {
            return "DAILY";
        }

        String normalized = groupBy.trim().toUpperCase();
        return "MONTHLY".equals(normalized) ? "MONTHLY" : "DAILY";
    }

    private boolean isWithinRange(LocalDate date, LocalDate fromDate, LocalDate toDate) {
        if (date == null) {
            return false;
        }
        return !date.isBefore(fromDate) && !date.isAfter(toDate);
    }

    private LocalDate getSafeOrderDate(Order order) {
        if (order == null || order.getOrderDate() == null) {
            return null;
        }
        return order.getOrderDate().toLocalDate();
    }

    private LocalDate getSafeUserCreatedDate(UserAccount user) {
        if (user == null || user.getCreatedAt() == null) {
            return null;
        }
        return user.getCreatedAt().toLocalDate();
    }

    private LocalDate getSafeReturnTrackingDate(ReturnRequest request) {
        if (request == null) {
            return null;
        }

        if (request.getResolvedAt() != null) {
            return request.getResolvedAt().toLocalDate();
        }

        if (request.getRequestedAt() != null) {
            return request.getRequestedAt().toLocalDate();
        }

        return null;
    }

    private LocalDate getSafeRefundDate(ReturnRequest request) {
        if (request == null) {
            return null;
        }

        if (request.getResolvedAt() != null) {
            return request.getResolvedAt().toLocalDate();
        }

        if (request.getRequestedAt() != null) {
            return request.getRequestedAt().toLocalDate();
        }

        return null;
    }

    private boolean isCompletedOrder(Order order) {
        if (order == null || order.getStatus() == null) {
            return false;
        }
        String status = order.getStatus().trim().toUpperCase();
        return "DELIVERED".equals(status) || "COMPLETED".equals(status);
    }

    private boolean isVnpayPaidOrder(Order order) {
        if (order == null) {
            return false;
        }

        String method = order.getPaymentMethod() != null ? order.getPaymentMethod().trim() : "";
        if (!"VNPAY".equalsIgnoreCase(method)) {
            return false;
        }

        String paymentStatus = order.getPaymentStatus() != null ? order.getPaymentStatus().trim() : "";
        return "PAID".equalsIgnoreCase(paymentStatus) || "PAID_FULL".equalsIgnoreCase(paymentStatus);
    }

    private boolean isProjectedOrder(Order order) {
        if (order == null || order.getStatus() == null) {
            return false;
        }
        String status = order.getStatus().trim().toUpperCase();
        return "PENDING".equals(status)
                || "PROCESSING".equals(status)
                || "SHIPPED".equals(status)
                || "DELIVERING".equals(status)
                || "PREORDER".equals(status);
    }

    private boolean isPendingOrder(Order order) {
        return order != null && "PENDING".equalsIgnoreCase(order.getStatus());
    }

    private boolean isProcessingOrder(Order order) {
        return order != null && "PROCESSING".equalsIgnoreCase(order.getStatus());
    }

    private boolean isShippingOrder(Order order) {
        if (order == null || order.getStatus() == null) {
            return false;
        }
        String status = order.getStatus().trim().toUpperCase();
        return "SHIPPED".equals(status) || "DELIVERING".equals(status) || "SHIPPING".equals(status);
    }

    private boolean isCancelledOrder(Order order) {
        if (order == null || order.getStatus() == null) {
            return false;
        }
        String status = order.getStatus().trim().toUpperCase();
        return "CANCELED".equals(status) || "CANCELLED".equals(status);
    }

    private boolean isRefundPending(ReturnRequest request) {
        return request != null && request.getStatus() == ReturnRequest.ReturnStatus.REFUND_PENDING;
    }

    private boolean isPendingReturnRequest(ReturnRequest request) {
        return request != null && request.getStatus() == ReturnRequest.ReturnStatus.PENDING;
    }

    private boolean isActuallyRefunded(ReturnRequest request) {
        if (request == null || request.getStatus() == null) {
            return false;
        }
        return request.getStatus() == ReturnRequest.ReturnStatus.REFUNDED
                || request.getStatus() == ReturnRequest.ReturnStatus.COMPLETED;
    }

    private BigDecimal sumOrderRevenue(List<Order> orders) {
        return orders.stream()
                .map(this::getOrderAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long sumSoldItems(List<Order> orders) {
        return orders.stream()
                .flatMap(order -> getSafeOrderItems(order).stream())
                .mapToLong(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();
    }

    private BigDecimal getOrderAmount(Order order) {
        if (order == null) {
            return BigDecimal.ZERO;
        }

        if (order.getFinalPrice() != null) {
            return order.getFinalPrice();
        }

        if (order.getTotalPrice() != null) {
            return order.getTotalPrice();
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal sumRefundAmount(List<ReturnRequest> requests) {
        return requests.stream()
                .map(this::calculateRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateRefundAmount(ReturnRequest request) {
        if (request == null || request.getOrderItem() == null) {
            return BigDecimal.ZERO;
        }

        OrderItem orderItem = request.getOrderItem();
        BigDecimal unitPrice = orderItem.getUnitPrice() != null ? orderItem.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal lensPrice = orderItem.getLensPrice() != null ? orderItem.getLensPrice() : BigDecimal.ZERO;
        int quantity = request.getReturnQuantity() != null ? request.getReturnQuantity() : 0;

        return unitPrice.add(lensPrice).multiply(BigDecimal.valueOf(quantity));
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    private double percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private Double calculateChangePercent(BigDecimal current, BigDecimal previous) {
        BigDecimal safeCurrent = current != null ? current : BigDecimal.ZERO;
        BigDecimal safePrevious = previous != null ? previous : BigDecimal.ZERO;

        if (safePrevious.compareTo(BigDecimal.ZERO) == 0) {
            return safeCurrent.compareTo(BigDecimal.ZERO) > 0 ? 100D : 0D;
        }

        return safeCurrent.subtract(safePrevious)
                .multiply(BigDecimal.valueOf(100))
                .divide(safePrevious, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private List<OrderStatusReportResponse> buildOrderStatusReport(
            long pendingOrders,
            long processingOrders,
            long shippingOrders,
            long completedOrders,
            long cancelledOrders
    ) {
        List<OrderStatusReportResponse> responses = new ArrayList<>();
        responses.add(OrderStatusReportResponse.builder().status("Pending").count(pendingOrders).build());
        responses.add(OrderStatusReportResponse.builder().status("Processing").count(processingOrders).build());
        responses.add(OrderStatusReportResponse.builder().status("Shipping").count(shippingOrders).build());
        responses.add(OrderStatusReportResponse.builder().status("Completed").count(completedOrders).build());
        responses.add(OrderStatusReportResponse.builder().status("Cancelled").count(cancelledOrders).build());
        return responses;
    }

    private List<ReturnStatusReportResponse> buildReturnStatusReport(List<ReturnRequest> returnRequests) {
        Map<ReturnRequest.ReturnStatus, Long> countMap = new EnumMap<>(ReturnRequest.ReturnStatus.class);

        for (ReturnRequest request : returnRequests) {
            ReturnRequest.ReturnStatus status = request.getStatus();
            if (status == null) {
                continue;
            }
            countMap.put(status, countMap.getOrDefault(status, 0L) + 1);
        }

        List<ReturnStatusReportResponse> responses = new ArrayList<>();
        for (ReturnRequest.ReturnStatus status : ReturnRequest.ReturnStatus.values()) {
            responses.add(ReturnStatusReportResponse.builder()
                    .status(status.name())
                    .count(countMap.getOrDefault(status, 0L))
                    .build());
        }

        return responses;
    }

    private List<DashboardTimePointResponse> buildTimeline(
            LocalDate fromDate,
            LocalDate toDate,
            String groupBy,
            List<Order> completedOrders,
            List<UserAccount> newCustomers,
            List<ReturnRequest> refundedRequests
    ) {
        LinkedHashMap<String, TimePointAccumulator> buckets = initializeBuckets(fromDate, toDate, groupBy);

        for (Order order : completedOrders) {
            LocalDate orderDate = getSafeOrderDate(order);
            if (orderDate == null) continue;

            String key = buildBucketKey(orderDate, groupBy);
            TimePointAccumulator bucket = buckets.get(key);
            if (bucket == null) continue;

            bucket.revenue = bucket.revenue.add(getOrderAmount(order));

            long soldQuantity = getSafeOrderItems(order).stream()
                    .mapToLong(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                    .sum();

            bucket.soldItems += soldQuantity;
        }

        for (UserAccount customer : newCustomers) {
            LocalDate customerDate = getSafeUserCreatedDate(customer);
            if (customerDate == null) continue;

            String key = buildBucketKey(customerDate, groupBy);
            TimePointAccumulator bucket = buckets.get(key);
            if (bucket == null) continue;

            bucket.customerRegistrations += 1;
        }

        for (ReturnRequest request : refundedRequests) {
            LocalDate refundDate = getSafeRefundDate(request);
            if (refundDate == null) continue;

            String key = buildBucketKey(refundDate, groupBy);
            TimePointAccumulator bucket = buckets.get(key);
            if (bucket == null) continue;

            bucket.refundedAmount = bucket.refundedAmount.add(calculateRefundAmount(request));
        }

        addVnpayRefundsToTimeline(buckets, fromDate, toDate, groupBy);

        return buckets.values().stream()
                .map(acc -> DashboardTimePointResponse.builder()
                        .label(acc.label)
                        .revenue(acc.revenue)
                        .customerRegistrations(acc.customerRegistrations)
                        .soldItems(acc.soldItems)
                        .refundedAmount(acc.refundedAmount)
                        .build())
                .toList();
    }

    private void addVnpayRefundsToTimeline(
            LinkedHashMap<String, TimePointAccumulator> buckets,
            LocalDate fromDate,
            LocalDate toDate,
            String groupBy
    ) {
        for (Map.Entry<String, TimePointAccumulator> entry : buckets.entrySet()) {
            LocalDate bucketStartDate;
            LocalDate bucketEndDate;

            if ("MONTHLY".equals(groupBy)) {
                YearMonth yearMonth = YearMonth.parse(entry.getKey());
                bucketStartDate = yearMonth.atDay(1);
                bucketEndDate = yearMonth.atEndOfMonth();

                if (bucketStartDate.isBefore(fromDate)) {
                    bucketStartDate = fromDate;
                }
                if (bucketEndDate.isAfter(toDate)) {
                    bucketEndDate = toDate;
                }
            } else {
                bucketStartDate = LocalDate.parse(entry.getKey());
                bucketEndDate = bucketStartDate;
            }

            LocalDateTime from = bucketStartDate.atStartOfDay();
            LocalDateTime to = bucketEndDate.atTime(23, 59, 59);

            BigDecimal refundedFromCancelledVnpayOrders = safeAmount(paymentRepository.sumVnpayRefundedAmountsBetween(from, to)).abs();
            entry.getValue().refundedAmount = entry.getValue().refundedAmount.add(refundedFromCancelledVnpayOrders);
        }
    }

    private LinkedHashMap<String, TimePointAccumulator> initializeBuckets(LocalDate fromDate, LocalDate toDate, String groupBy) {
        LinkedHashMap<String, TimePointAccumulator> buckets = new LinkedHashMap<>();

        if ("MONTHLY".equals(groupBy)) {
            YearMonth start = YearMonth.from(fromDate);
            YearMonth end = YearMonth.from(toDate);

            while (!start.isAfter(end)) {
                String key = start.toString();
                String label = start.format(DateTimeFormatter.ofPattern("MM/yyyy"));
                buckets.put(key, new TimePointAccumulator(label));
                start = start.plusMonths(1);
            }

            return buckets;
        }

        LocalDate cursor = fromDate;
        while (!cursor.isAfter(toDate)) {
            String key = cursor.toString();
            String label = cursor.format(DateTimeFormatter.ofPattern("dd/MM"));
            buckets.put(key, new TimePointAccumulator(label));
            cursor = cursor.plusDays(1);
        }

        return buckets;
    }

    private String buildBucketKey(LocalDate date, String groupBy) {
        if ("MONTHLY".equals(groupBy)) {
            return YearMonth.from(date).toString();
        }
        return date.toString();
    }

    private List<ProductReportResponse> buildTopProductsByQuantity(List<Order> completedOrders) {
        Map<String, ProductAccumulator> map = buildProductMap(completedOrders, Collections.emptyList());

        return map.values().stream()
                .sorted(Comparator.comparing(ProductAccumulator::getQuantitySold).reversed())
                .limit(8)
                .map(item -> ProductReportResponse.builder()
                        .productName(item.productName)
                        .quantitySold(item.quantitySold)
                        .revenue(item.revenue)
                .completedRevenue(item.completedRevenue)
                .pendingRevenue(item.pendingRevenue)
                .refundedRevenue(item.refundedRevenue)
                        .build())
                .toList();
    }

            private List<ProductReportResponse> buildTopFramesByQuantity(List<Order> completedOrders) {
            Map<String, ProductAccumulator> map = buildFrameProductMap(completedOrders, Collections.emptyList());

            return map.values().stream()
                .sorted(Comparator.comparing(ProductAccumulator::getQuantitySold).reversed())
                .limit(8)
                .map(item -> ProductReportResponse.builder()
                    .productName(item.productName)
                    .quantitySold(item.quantitySold)
                    .revenue(item.revenue)
                    .completedRevenue(item.completedRevenue)
                    .pendingRevenue(item.pendingRevenue)
                    .refundedRevenue(item.refundedRevenue)
                    .build())
                .toList();
            }

    private List<ProductReportResponse> buildTopProductsByRevenue(List<Order> ordersInRange, List<ReturnRequest> returnRequests) {
        Map<String, ProductAccumulator> map = buildProductMap(ordersInRange, returnRequests);

        return map.values().stream()
                .sorted(Comparator.comparing(ProductAccumulator::getRevenue).reversed())
                .limit(8)
                .map(item -> ProductReportResponse.builder()
                        .productName(item.productName)
                        .quantitySold(item.quantitySold)
                        .revenue(item.revenue)
                .completedRevenue(item.completedRevenue)
                .pendingRevenue(item.pendingRevenue)
                .refundedRevenue(item.refundedRevenue)
                        .build())
                .toList();
    }

            private List<ProductReportResponse> buildTopFramesByRevenue(List<Order> ordersInRange, List<ReturnRequest> returnRequests) {
            Map<String, ProductAccumulator> map = buildFrameProductMap(ordersInRange, returnRequests);

            return map.values().stream()
                .sorted(Comparator.comparing(ProductAccumulator::getRevenue).reversed())
                .limit(8)
                .map(item -> ProductReportResponse.builder()
                    .productName(item.productName)
                    .quantitySold(item.quantitySold)
                    .revenue(item.revenue)
                    .completedRevenue(item.completedRevenue)
                    .pendingRevenue(item.pendingRevenue)
                    .refundedRevenue(item.refundedRevenue)
                    .build())
                .toList();
            }

            private List<LensReportResponse> buildTopLensesByQuantity(List<Order> completedOrders) {
            Map<String, LensAccumulator> map = buildLensMap(completedOrders, Collections.emptyList());

            return map.values().stream()
                .sorted(Comparator.comparing(LensAccumulator::getQuantitySold).reversed())
                .limit(8)
                .map(item -> LensReportResponse.builder()
                    .lensType(item.lensType)
                    .quantitySold(item.quantitySold)
                    .revenue(item.revenue)
                    .completedRevenue(item.completedRevenue)
                    .pendingRevenue(item.pendingRevenue)
                    .refundedRevenue(item.refundedRevenue)
                    .build())
                .toList();
            }

            private List<LensReportResponse> buildTopLensesByRevenue(List<Order> ordersInRange, List<ReturnRequest> returnRequests) {
            Map<String, LensAccumulator> map = buildLensMap(ordersInRange, returnRequests);

            return map.values().stream()
                .sorted(Comparator.comparing(LensAccumulator::getRevenue).reversed())
                .limit(8)
                .map(item -> LensReportResponse.builder()
                    .lensType(item.lensType)
                    .quantitySold(item.quantitySold)
                    .revenue(item.revenue)
                    .completedRevenue(item.completedRevenue)
                    .pendingRevenue(item.pendingRevenue)
                    .refundedRevenue(item.refundedRevenue)
                    .build())
                .toList();
            }

            private List<LensReportResponse> buildLensSalesReport(List<Order> ordersInRange, List<ReturnRequest> returnRequests) {
            Map<String, LensAccumulator> map = buildLensMap(ordersInRange, returnRequests);

            return map.values().stream()
                .sorted(Comparator.comparing(LensAccumulator::getQuantitySold).reversed()
                    .thenComparing(LensAccumulator::getLensType, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(item -> LensReportResponse.builder()
                    .lensType(item.lensType)
                    .quantitySold(item.quantitySold)
                    .revenue(item.revenue)
                    .completedRevenue(item.completedRevenue)
                    .pendingRevenue(item.pendingRevenue)
                    .refundedRevenue(item.refundedRevenue)
                    .build())
                .toList();
            }

    private List<CustomerPurchaseSummaryResponse> buildTopCustomersBySpending(List<Order> completedOrders) {
        Map<String, CustomerAccumulator> customerMap = new HashMap<>();

        for (Order order : completedOrders) {
            UserAccount user = order.getUser();
            String key = user != null && user.getUserId() != null
                    ? "U-" + user.getUserId()
                    : "GUEST-" + normalizeGuestName(order.getFullName());

            CustomerAccumulator accumulator = customerMap.computeIfAbsent(
                    key,
                    ignored -> new CustomerAccumulator(
                            user != null ? user.getUserId() : null,
                            resolveCustomerName(order, user))
            );

            accumulator.orderCount += 1;
            accumulator.totalSpent = accumulator.totalSpent.add(getOrderAmount(order));
        }

        return customerMap.values().stream()
                .sorted(Comparator.comparing(CustomerAccumulator::getTotalSpent).reversed())
                .limit(TOP_CUSTOMERS_LIMIT)
                .map(item -> CustomerPurchaseSummaryResponse.builder()
                        .userId(item.userId)
                        .customerName(item.customerName)
                        .orderCount(item.orderCount)
                        .totalSpent(item.totalSpent)
                        .build())
                .toList();
    }

    private List<CustomerInsightResponse> buildCustomerInsights(List<Order> ordersInRange) {
        Map<String, CustomerInsightAccumulator> customerMap = new HashMap<>();

        for (Order order : ordersInRange) {
            UserAccount user = order != null ? order.getUser() : null;
            String key = user != null && user.getUserId() != null
                    ? "U-" + user.getUserId()
                    : "GUEST-" + normalizeGuestName(order != null ? order.getFullName() : null);

            CustomerInsightAccumulator accumulator = customerMap.computeIfAbsent(
                    key,
                    ignored -> new CustomerInsightAccumulator(
                            user != null ? user.getUserId() : null,
                            resolveCustomerName(order, user)
                    )
            );

            accumulator.totalOrders += 1;

            if (isCompletedOrder(order)) {
                accumulator.completedOrders += 1;
                accumulator.totalSpent = accumulator.totalSpent.add(getOrderAmount(order));

                for (OrderItem item : getSafeOrderItems(order)) {
                    String productName = item != null ? item.getProductName() : null;
                    if (productName == null || productName.isBlank()) {
                        productName = "Unknown Product";
                    }
                    long quantity = item != null && item.getQuantity() != null ? item.getQuantity() : 0L;
                    accumulator.productQuantities.put(
                            productName,
                            accumulator.productQuantities.getOrDefault(productName, 0L) + quantity
                    );
                }
            }

            if (isCancelledOrder(order)) {
                accumulator.cancelledOrders += 1;
            }
        }

        return customerMap.values().stream()
                .map(acc -> {
                    Map.Entry<String, Long> favorite = acc.productQuantities.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .orElse(null);

                    return CustomerInsightResponse.builder()
                            .userId(acc.userId)
                            .customerName(acc.customerName)
                            .totalOrders(acc.totalOrders)
                            .completedOrders(acc.completedOrders)
                            .cancelledOrders(acc.cancelledOrders)
                            .totalSpent(acc.totalSpent)
                            .favoriteProductName(favorite != null ? favorite.getKey() : null)
                            .favoriteProductQuantity(favorite != null ? favorite.getValue() : 0L)
                            .build();
                })
                .sorted(Comparator.comparing(CustomerInsightResponse::getTotalSpent).reversed())
                .limit(50)
                .toList();
    }

    private List<ProductInventoryReportResponse> buildProductInventoryReport(List<Order> ordersInRange, List<ReturnRequest> returnRequests) {
        Map<String, ProductInventoryAccumulator> inventoryMap = new LinkedHashMap<>();

        for (ProductVariant variant : productVariantRepository.findByDeletedFalse()) {
            if (variant == null || variant.getProduct() == null || variant.getProduct().getProductId() == null) {
                continue;
            }

            Long productId = variant.getProduct().getProductId();
            String key = buildInventoryKey(productId, variant.getColor());
            ProductInventoryAccumulator accumulator = inventoryMap.computeIfAbsent(
                    key,
                    ignored -> new ProductInventoryAccumulator(variant.getProduct().getName(), normalizeColorLabel(variant.getColor()))
            );

            if (accumulator.productName == null || accumulator.productName.isBlank()) {
                accumulator.productName = variant.getProduct().getName();
            }

            if (accumulator.productType == null && variant.getProduct().getProductType() != null) {
                accumulator.productType = variant.getProduct().getProductType();
            }

            if (accumulator.color == null || accumulator.color.isBlank()) {
                accumulator.color = normalizeColorLabel(variant.getColor());
            }

            accumulator.inStockQuantity += variant.getStockQuantity() != null ? variant.getStockQuantity() : 0L;
        }

        for (Order order : ordersInRange) {
            for (OrderItem item : getSafeOrderItems(order)) {
                String key = buildInventoryKey(item);
                ProductInventoryAccumulator accumulator = inventoryMap.computeIfAbsent(
                        key,
                        ignored -> new ProductInventoryAccumulator(resolveInventoryProductName(item), resolveInventoryColor(item))
                );

                if (accumulator.productName == null || accumulator.productName.isBlank()) {
                    accumulator.productName = resolveInventoryProductName(item);
                }

                if (accumulator.color == null || accumulator.color.isBlank()) {
                    accumulator.color = resolveInventoryColor(item);
                }

                if (accumulator.productType == null) {
                    try {
                        ProductVariant variant = item != null ? item.getVariant() : null;
                        if (variant != null && variant.getProduct() != null && variant.getProduct().getProductType() != null) {
                            accumulator.productType = variant.getProduct().getProductType();
                        }
                    } catch (Exception ignored) {
                        // ignore
                    }
                }

                long quantity = item != null && item.getQuantity() != null ? item.getQuantity() : 0L;

                BigDecimal revenue = calculateItemRevenue(item, quantity);
                accumulator.revenue = accumulator.revenue.add(revenue);
                if (isCompletedOrder(order)) {
                    accumulator.completedRevenue = accumulator.completedRevenue.add(revenue);
                } else if (isProjectedOrder(order)) {
                    accumulator.pendingRevenue = accumulator.pendingRevenue.add(revenue);
                }

                if (isCompletedOrder(order)) {
                    accumulator.purchasedQuantity += quantity;
                }

                if (Boolean.TRUE.equals(item != null ? item.getIsPreorder() : null)) {
                    accumulator.preorderQuantity += quantity;
                }
            }
        }

        if (returnRequests != null) {
            for (ReturnRequest request : returnRequests) {
                if (request == null || request.getOrderItem() == null) {
                    continue;
                }
                if (request.getStatus() != ReturnRequest.ReturnStatus.REFUND_PENDING
                        && request.getStatus() != ReturnRequest.ReturnStatus.REFUNDED
                        && request.getStatus() != ReturnRequest.ReturnStatus.COMPLETED) {
                    continue;
                }

                OrderItem item = request.getOrderItem();
                String key = buildInventoryKey(item);
                ProductInventoryAccumulator accumulator = inventoryMap.computeIfAbsent(
                        key,
                    ignored -> new ProductInventoryAccumulator(resolveInventoryProductName(item), resolveInventoryColor(item))
                );

                if (accumulator.productName == null || accumulator.productName.isBlank()) {
                    accumulator.productName = resolveInventoryProductName(item);
                }

                if (accumulator.color == null || accumulator.color.isBlank()) {
                    accumulator.color = resolveInventoryColor(item);
                }

                if (accumulator.productType == null) {
                    try {
                        ProductVariant variant = item != null ? item.getVariant() : null;
                        if (variant != null && variant.getProduct() != null && variant.getProduct().getProductType() != null) {
                            accumulator.productType = variant.getProduct().getProductType();
                        }
                    } catch (Exception ignored) {
                        // ignore
                    }
                }

                long quantity = request.getReturnQuantity() != null ? request.getReturnQuantity() : 0L;
                BigDecimal refundedRevenue = calculateRefundAmount(request);

                accumulator.refundedRevenue = accumulator.refundedRevenue.add(refundedRevenue);
                accumulator.revenue = accumulator.revenue.add(refundedRevenue);
            }
        }

        return inventoryMap.values().stream()
                .sorted(Comparator.comparing(ProductInventoryAccumulator::getPurchasedQuantity).reversed()
                        .thenComparing(ProductInventoryAccumulator::getProductName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(item -> ProductInventoryReportResponse.builder()
                        .productName(item.productName)
                .productType(item.productType != null ? item.productType.name() : null)
                        .color(item.color)
                        .purchasedQuantity(item.purchasedQuantity)
                        .inStockQuantity(item.inStockQuantity)
                        .preorderQuantity(item.preorderQuantity)
                    .revenue(item.revenue)
                    .completedRevenue(item.completedRevenue)
                    .pendingRevenue(item.pendingRevenue)
                    .refundedRevenue(item.refundedRevenue)
                        .build())
                .toList();
    }

    private String buildInventoryKey(OrderItem item) {
        try {
            ProductVariant variant = item != null ? item.getVariant() : null;
            if (variant != null && variant.getProduct() != null && variant.getProduct().getProductId() != null) {
                return buildInventoryKey(variant.getProduct().getProductId(), variant.getColor());
            }
        } catch (Exception ignored) {
            // ignore
        }

        if (item != null && item.getProductId() != null) {
            return buildInventoryKey(item.getProductId(), null);
        }

        return "N-" + normalizeInventoryName(resolveInventoryProductName(item));
    }

    private String buildInventoryKey(Long productId, String color) {
        if (productId == null) {
            return "P-UNKNOWN";
        }
        return "P-" + productId + "-C-" + normalizeColorKey(color);
    }

    private String normalizeColorKey(String color) {
        if (color == null || color.isBlank()) {
            return "UNKNOWN";
        }
        return color.trim().toUpperCase();
    }

    private String normalizeColorLabel(String color) {
        if (color == null || color.isBlank()) {
            return "-";
        }
        String normalized = color.trim();
        return normalized.isBlank() ? "-" : normalized;
    }

    private String resolveInventoryColor(OrderItem item) {
        try {
            ProductVariant variant = item != null ? item.getVariant() : null;
            if (variant != null) {
                return normalizeColorLabel(variant.getColor());
            }
        } catch (Exception ignored) {
            // ignore
        }
        return "-";
    }

    private String resolveInventoryProductName(OrderItem item) {
        if (item == null || item.getProductName() == null || item.getProductName().isBlank()) {
            return "Unknown Product";
        }
        return item.getProductName().trim();
    }

    private String normalizeInventoryName(String productName) {
        if (productName == null || productName.isBlank()) {
            return "UNKNOWN";
        }
        return productName.trim().toUpperCase();
    }

    private String resolveCustomerName(Order order, UserAccount user) {
        if (user != null && user.getName() != null && !user.getName().isBlank()) {
            return user.getName().trim();
        }
        if (order != null && order.getFullName() != null && !order.getFullName().isBlank()) {
            return order.getFullName().trim();
        }
        return "Guest";
    }

    private String normalizeGuestName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "UNKNOWN";
        }
        return fullName.trim().toUpperCase();
    }

    private Map<String, ProductAccumulator> buildProductMap(List<Order> ordersInRange, List<ReturnRequest> returnRequests) {
        Map<String, ProductAccumulator> map = new HashMap<>();

        for (Order order : ordersInRange) {
            for (OrderItem item : getSafeOrderItems(order)) {
                String productName = item.getProductName();
                if (productName == null || productName.isBlank()) {
                    productName = "Unknown Product";
                }

                ProductAccumulator accumulator = map.computeIfAbsent(productName, ProductAccumulator::new);

                long quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                BigDecimal revenue = calculateItemRevenue(item, quantity);

                accumulator.quantitySold += quantity;
                accumulator.revenue = accumulator.revenue.add(revenue);

                if (isCompletedOrder(order)) {
                    accumulator.completedRevenue = accumulator.completedRevenue.add(revenue);
                } else if (isProjectedOrder(order)) {
                    accumulator.pendingRevenue = accumulator.pendingRevenue.add(revenue);
                }
            }
        }

        for (ReturnRequest request : returnRequests) {
            if (request == null || request.getOrderItem() == null) {
                continue;
            }

                if (request.getStatus() != ReturnRequest.ReturnStatus.REFUND_PENDING
                    && request.getStatus() != ReturnRequest.ReturnStatus.REFUNDED
                    && request.getStatus() != ReturnRequest.ReturnStatus.COMPLETED) {
                continue;
            }

            OrderItem item = request.getOrderItem();
            String productName = item.getProductName();
            if (productName == null || productName.isBlank()) {
                productName = "Unknown Product";
            }

            ProductAccumulator accumulator = map.computeIfAbsent(productName, ProductAccumulator::new);
            BigDecimal refundedRevenue = calculateRefundAmount(request);
            accumulator.refundedRevenue = accumulator.refundedRevenue.add(refundedRevenue);
            accumulator.revenue = accumulator.revenue.add(refundedRevenue);
        }

        return map;
    }

    private Map<String, ProductAccumulator> buildFrameProductMap(List<Order> ordersInRange, List<ReturnRequest> returnRequests) {
        Map<String, ProductAccumulator> map = new HashMap<>();

        for (Order order : ordersInRange) {
            for (OrderItem item : getSafeOrderItems(order)) {
                if (isLensProduct(item)) {
                    continue;
                }
                String productName = item.getProductName();
                if (productName == null || productName.isBlank()) {
                    productName = "Unknown Product";
                }

                ProductAccumulator accumulator = map.computeIfAbsent(productName, ProductAccumulator::new);

                long quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                BigDecimal revenue = calculateFrameRevenue(item, quantity);

                accumulator.quantitySold += quantity;
                accumulator.revenue = accumulator.revenue.add(revenue);

                if (isCompletedOrder(order)) {
                    accumulator.completedRevenue = accumulator.completedRevenue.add(revenue);
                } else if (isProjectedOrder(order)) {
                    accumulator.pendingRevenue = accumulator.pendingRevenue.add(revenue);
                }
            }
        }

        for (ReturnRequest request : returnRequests) {
            if (request == null || request.getOrderItem() == null) {
                continue;
            }

            if (request.getStatus() != ReturnRequest.ReturnStatus.REFUND_PENDING
                    && request.getStatus() != ReturnRequest.ReturnStatus.REFUNDED
                    && request.getStatus() != ReturnRequest.ReturnStatus.COMPLETED) {
                continue;
            }

            OrderItem item = request.getOrderItem();
            if (isLensProduct(item)) {
                continue;
            }
            String productName = item.getProductName();
            if (productName == null || productName.isBlank()) {
                productName = "Unknown Product";
            }

            ProductAccumulator accumulator = map.computeIfAbsent(productName, ProductAccumulator::new);
            long quantity = request.getReturnQuantity() != null ? request.getReturnQuantity() : 0L;
            BigDecimal refundedRevenue = calculateFrameRevenue(item, quantity);
            accumulator.refundedRevenue = accumulator.refundedRevenue.add(refundedRevenue);
            accumulator.revenue = accumulator.revenue.add(refundedRevenue);
        }

        return map;
    }

    private Map<String, LensAccumulator> buildLensMap(List<Order> ordersInRange, List<ReturnRequest> returnRequests) {
        Map<String, LensAccumulator> map = new HashMap<>();

        for (Order order : ordersInRange) {
            for (OrderItem item : getSafeOrderItems(order)) {
                if (Boolean.TRUE.equals(item != null ? item.getIsPreorder() : null)) {
                    continue;
                }

                if (!isLensRelevant(item)) {
                    continue;
                }

                String lensType = normalizeLensType(item);
                LensAccumulator accumulator = map.computeIfAbsent(lensType, LensAccumulator::new);

                long quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                BigDecimal revenue = calculateLensRevenue(item, quantity);

                if (revenue.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                accumulator.quantitySold += quantity;
                accumulator.revenue = accumulator.revenue.add(revenue);

                if (isCompletedOrder(order)) {
                    accumulator.completedRevenue = accumulator.completedRevenue.add(revenue);
                } else if (isProjectedOrder(order)) {
                    accumulator.pendingRevenue = accumulator.pendingRevenue.add(revenue);
                }
            }
        }

        for (ReturnRequest request : returnRequests) {
            if (request == null || request.getOrderItem() == null) {
                continue;
            }

            if (request.getStatus() != ReturnRequest.ReturnStatus.REFUND_PENDING
                    && request.getStatus() != ReturnRequest.ReturnStatus.REFUNDED
                    && request.getStatus() != ReturnRequest.ReturnStatus.COMPLETED) {
                continue;
            }

            OrderItem item = request.getOrderItem();
            if (Boolean.TRUE.equals(item != null ? item.getIsPreorder() : null)) {
                continue;
            }

            if (!isLensRelevant(item)) {
                continue;
            }

            String lensType = normalizeLensType(item);
            LensAccumulator accumulator = map.computeIfAbsent(lensType, LensAccumulator::new);
            long quantity = request.getReturnQuantity() != null ? request.getReturnQuantity() : 0L;
            BigDecimal refundedRevenue = calculateLensRevenue(item, quantity);

            if (refundedRevenue.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            accumulator.refundedRevenue = accumulator.refundedRevenue.add(refundedRevenue);
            accumulator.revenue = accumulator.revenue.add(refundedRevenue);
        }

        return map;
    }

    private boolean isLensRelevant(OrderItem item) {
        return isLensProduct(item) || hasLensOptionSnapshot(item);
    }

    private boolean isLensProduct(OrderItem item) {
        if (item == null) {
            return false;
        }

        try {
            ProductVariant variant = item.getVariant();
            if (variant != null && variant.getProduct() != null && variant.getProduct().getProductType() != null) {
                return variant.getProduct().getProductType() == Product.ProductType.LENS;
            }
        } catch (Exception ignored) {
            // fall back to name-based detection
        }

        String name = item.getProductName();
        if (name == null) {
            return false;
        }
        String lower = name.trim().toLowerCase();
        return lower.contains("lens") || lower.contains("tròng") || lower.contains("trong") || lower.contains("thấu");
    }

    private boolean hasLensOptionSnapshot(OrderItem item) {
        if (item == null) {
            return false;
        }
        if (item.getLensOptionId() != null) {
            return true;
        }
        BigDecimal lensPrice = item.getLensPrice() != null ? item.getLensPrice() : BigDecimal.ZERO;
        if (lensPrice.compareTo(BigDecimal.ZERO) > 0) {
            return true;
        }
        String lensType = item.getLensType();
        return lensType != null && !lensType.isBlank();
    }

    private String normalizeLensType(OrderItem item) {
        if (item == null) {
            return "Unknown Lens";
        }

        String lensType = item.getLensType();
        if (lensType != null && !lensType.isBlank()) {
            return lensType.trim();
        }

        if (isLensProduct(item)) {
            String name = item.getProductName();
            if (name != null && !name.isBlank()) {
                return name.trim();
            }
        }

        return "Unknown Lens";
    }

    private BigDecimal calculateFrameRevenue(OrderItem item, long quantity) {
        if (isLensProduct(item)) {
            return BigDecimal.ZERO;
        }

        BigDecimal unitPrice = item != null && item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal lensPrice = item != null && item.getLensPrice() != null ? item.getLensPrice() : BigDecimal.ZERO;

        // In current cart/order flow, unitPrice already includes lensPrice when a lens option is chosen.
        // So frame revenue should exclude the lens part.
        if (hasLensOptionSnapshot(item) && lensPrice.compareTo(BigDecimal.ZERO) > 0 && unitPrice.compareTo(lensPrice) >= 0) {
            unitPrice = unitPrice.subtract(lensPrice);
        }

        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            unitPrice = BigDecimal.ZERO;
        }

        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private BigDecimal calculateLensRevenue(OrderItem item, long quantity) {
        if (item == null) {
            return BigDecimal.ZERO;
        }

        // Standalone lens products: revenue is in unitPrice
        if (isLensProduct(item)) {
            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }

        BigDecimal lensPrice = item.getLensPrice() != null ? item.getLensPrice() : BigDecimal.ZERO;
        return lensPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private BigDecimal calculateItemRevenue(OrderItem item, long quantity) {
        // unitPrice snapshot already represents the chargeable unit amount (frame + lens option, if any)
        BigDecimal unitPrice = item != null && item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private List<OrderItem> getSafeOrderItems(Order order) {
        if (order == null || order.getOrderItems() == null) {
            return Collections.emptyList();
        }
        return order.getOrderItems();
    }

    private static class TimePointAccumulator {
        private final String label;
        private BigDecimal revenue = BigDecimal.ZERO;
        private long customerRegistrations = 0L;
        private long soldItems = 0L;
        private BigDecimal refundedAmount = BigDecimal.ZERO;

        private TimePointAccumulator(String label) {
            this.label = label;
        }
    }

    private static class ProductAccumulator {
        private final String productName;
        private long quantitySold = 0L;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal completedRevenue = BigDecimal.ZERO;
        private BigDecimal pendingRevenue = BigDecimal.ZERO;
        private BigDecimal refundedRevenue = BigDecimal.ZERO;

        private ProductAccumulator(String productName) {
            this.productName = productName;
        }

        public long getQuantitySold() {
            return quantitySold;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }
    }

    private static class LensAccumulator {
        private final String lensType;
        private long quantitySold = 0L;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal completedRevenue = BigDecimal.ZERO;
        private BigDecimal pendingRevenue = BigDecimal.ZERO;
        private BigDecimal refundedRevenue = BigDecimal.ZERO;

        private LensAccumulator(String lensType) {
            this.lensType = lensType;
        }

        public long getQuantitySold() {
            return quantitySold;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public String getLensType() {
            return lensType;
        }
    }

    private static class ProductInventoryAccumulator {
        private String productName;
        private Product.ProductType productType;
        private String color;
        private long purchasedQuantity = 0L;
        private long inStockQuantity = 0L;
        private long preorderQuantity = 0L;

        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal completedRevenue = BigDecimal.ZERO;
        private BigDecimal pendingRevenue = BigDecimal.ZERO;
        private BigDecimal refundedRevenue = BigDecimal.ZERO;

        private ProductInventoryAccumulator(String productName, String color) {
            this.productName = productName;
            this.color = color;
        }

        public long getPurchasedQuantity() {
            return purchasedQuantity;
        }

        public String getProductName() {
            return productName;
        }
    }

    private static class CustomerAccumulator {
        private final Long userId;
        private final String customerName;
        private long orderCount = 0L;
        private BigDecimal totalSpent = BigDecimal.ZERO;

        private CustomerAccumulator(Long userId, String customerName) {
            this.userId = userId;
            this.customerName = customerName;
        }

        public BigDecimal getTotalSpent() {
            return totalSpent;
        }
    }

    private static class CustomerInsightAccumulator {
        private final Long userId;
        private final String customerName;
        private long totalOrders = 0L;
        private long completedOrders = 0L;
        private long cancelledOrders = 0L;
        private BigDecimal totalSpent = BigDecimal.ZERO;
        private final Map<String, Long> productQuantities = new HashMap<>();

        private CustomerInsightAccumulator(Long userId, String customerName) {
            this.userId = userId;
            this.customerName = customerName;
        }
    }
}