package com.fpt.glasseshop.service.impl;

import com.fpt.glasseshop.entity.Order;
import com.fpt.glasseshop.entity.OrderItem;
import com.fpt.glasseshop.entity.ProductVariant;
import com.fpt.glasseshop.entity.ReturnRequest;
import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.entity.dto.response.AdminDashboardAnalyticsResponse;
import com.fpt.glasseshop.entity.dto.response.AdminDashboardSummaryResponse;
import com.fpt.glasseshop.entity.dto.response.CustomerPurchaseSummaryResponse;
import com.fpt.glasseshop.entity.dto.response.DashboardTimePointResponse;
import com.fpt.glasseshop.entity.dto.response.OrderStatusReportResponse;
import com.fpt.glasseshop.entity.dto.response.ProductInventoryReportResponse;
import com.fpt.glasseshop.entity.dto.response.ProductReportResponse;
import com.fpt.glasseshop.entity.dto.response.ReturnStatusReportResponse;
import com.fpt.glasseshop.entity.dto.response.RevenueResponse;
import com.fpt.glasseshop.repository.OrderRepository;
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

@Service
public class ReportServiceImpl implements ReportService {

    private static final int TOP_CUSTOMERS_LIMIT = 10;

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

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

        BigDecimal grossRevenue = safeAmount(orderRepo.calculateGrossRevenueBetween(from, to));
        BigDecimal refundedAmount = safeAmount(returnRequestRepository.sumRefundAmountBetween(
                List.of(ReturnRequest.ReturnStatus.REFUND_PENDING, ReturnRequest.ReturnStatus.REFUNDED),
                from,
                to
        ));
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

        List<Order> previousOrdersInRange = allOrders.stream()
                .filter(order -> isWithinRange(getSafeOrderDate(order), previousFromDate, previousToDate))
                .toList();

        List<Order> currentCompletedOrders = currentOrdersInRange.stream()
                .filter(this::isCompletedOrder)
                .toList();

        List<Order> previousCompletedOrders = previousOrdersInRange.stream()
                .filter(this::isCompletedOrder)
                .toList();

        BigDecimal grossRevenue = sumOrderRevenue(currentCompletedOrders);
        BigDecimal previousRevenue = sumOrderRevenue(previousCompletedOrders);

        long soldItems = sumSoldItems(currentCompletedOrders);
        long previousSoldItems = sumSoldItems(previousCompletedOrders);

        BigDecimal pendingRevenue = currentOrdersInRange.stream()
                .filter(this::isProjectedOrder)
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
        BigDecimal netRevenue = grossRevenue.subtract(refundedAmount);

        long pendingOrders = currentOrdersInRange.stream().filter(this::isPendingOrder).count();
        long processingOrders = currentOrdersInRange.stream().filter(this::isProcessingOrder).count();
        long shippingOrders = currentOrdersInRange.stream().filter(this::isShippingOrder).count();
        long cancelledOrders = currentOrdersInRange.stream().filter(this::isCancelledOrder).count();
        long completedOrders = currentCompletedOrders.size();
        long totalOrders = currentOrdersInRange.size();

        long refundPendingCount = currentReturnRequests.stream().filter(this::isRefundPending).count();
        long refundedCount = currentRefundedReturns.size();
        long pendingReturnRequests = currentReturnRequests.stream().filter(this::isPendingReturnRequest).count();
        long totalReturnRequests = currentReturnRequests.size();

        double refundRate = percentage(refundedCount, totalOrders);
        double completionRate = percentage(completedOrders, totalOrders);

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
                .totalCustomers(totalCustomers)
                .newCustomers(newCustomers)
                .soldItems(soldItems)
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .pendingOrders(pendingOrders)
                .processingOrders(processingOrders)
                .shippingOrders(shippingOrders)
                .cancelledOrders(cancelledOrders)
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
                        currentCompletedOrders,
                        currentNewCustomers,
                        currentRefundedReturns
                ))
                .bestSellingProductsByQuantity(buildTopProductsByQuantity(currentCompletedOrders))
                .bestSellingProductsByRevenue(buildTopProductsByRevenue(currentOrdersInRange, currentReturnRequests))
                .productInventoryReport(buildProductInventoryReport(currentOrdersInRange))
                .topCustomersBySpending(buildTopCustomersBySpending(currentCompletedOrders))
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

    private List<ProductInventoryReportResponse> buildProductInventoryReport(List<Order> ordersInRange) {
        Map<String, ProductInventoryAccumulator> inventoryMap = new LinkedHashMap<>();

        for (ProductVariant variant : productVariantRepository.findByDeletedFalse()) {
            if (variant == null || variant.getProduct() == null || variant.getProduct().getProductId() == null) {
                continue;
            }

            Long productId = variant.getProduct().getProductId();
            ProductInventoryAccumulator accumulator = inventoryMap.computeIfAbsent(
                    productId.toString(),
                    ignored -> new ProductInventoryAccumulator(variant.getProduct().getName())
            );

            if (accumulator.productName == null || accumulator.productName.isBlank()) {
                accumulator.productName = variant.getProduct().getName();
            }

            if (accumulator.color == null || accumulator.color.isBlank()) {
                accumulator.color = variant.getColor();
            }

            accumulator.inStockQuantity += variant.getStockQuantity() != null ? variant.getStockQuantity() : 0L;
        }

        for (Order order : ordersInRange) {
            for (OrderItem item : getSafeOrderItems(order)) {
                String key = buildInventoryKey(item);
                ProductInventoryAccumulator accumulator = inventoryMap.computeIfAbsent(
                        key,
                        ignored -> new ProductInventoryAccumulator(resolveInventoryProductName(item))
                );

                if (accumulator.productName == null || accumulator.productName.isBlank()) {
                    accumulator.productName = resolveInventoryProductName(item);
                }

                long quantity = item != null && item.getQuantity() != null ? item.getQuantity() : 0L;

                if (isCompletedOrder(order)) {
                    accumulator.purchasedQuantity += quantity;
                }

                if (Boolean.TRUE.equals(item != null ? item.getIsPreorder() : null)) {
                    accumulator.preorderQuantity += quantity;
                }
            }
        }

        return inventoryMap.values().stream()
                .sorted(Comparator.comparing(ProductInventoryAccumulator::getPurchasedQuantity).reversed()
                        .thenComparing(ProductInventoryAccumulator::getProductName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(item -> ProductInventoryReportResponse.builder()
                        .productName(item.productName)
                        .color(item.color)
                        .purchasedQuantity(item.purchasedQuantity)
                        .inStockQuantity(item.inStockQuantity)
                        .preorderQuantity(item.preorderQuantity)
                        .build())
                .toList();
    }

    private String buildInventoryKey(OrderItem item) {
        if (item != null && item.getProductId() != null) {
            return "P-" + item.getProductId();
        }

        return "N-" + normalizeInventoryName(resolveInventoryProductName(item));
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

    private BigDecimal calculateItemRevenue(OrderItem item, long quantity) {
        BigDecimal unitPrice = item != null && item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal lensPrice = item != null && item.getLensPrice() != null ? item.getLensPrice() : BigDecimal.ZERO;
        return unitPrice.add(lensPrice).multiply(BigDecimal.valueOf(quantity));
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

    private static class ProductInventoryAccumulator {
        private String productName;
        private String color;
        private long purchasedQuantity = 0L;
        private long inStockQuantity = 0L;
        private long preorderQuantity = 0L;

        private ProductInventoryAccumulator(String productName) {
            this.productName = productName;
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
}