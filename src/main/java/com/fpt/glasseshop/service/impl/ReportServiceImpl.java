package com.fpt.glasseshop.service.impl;

import com.fpt.glasseshop.entity.Order;
import com.fpt.glasseshop.entity.OrderItem;
import com.fpt.glasseshop.entity.ReturnRequest;
import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.entity.dto.response.*;
import com.fpt.glasseshop.repository.OrderRepository;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private static final int TOP_CUSTOMERS_LIMIT = 20;

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

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

        BigDecimal totalRevenue = sumOrderRevenue(currentCompletedOrders);
        BigDecimal previousRevenue = sumOrderRevenue(previousCompletedOrders);

        long soldItems = sumSoldItems(currentCompletedOrders);
        long previousSoldItems = sumSoldItems(previousCompletedOrders);

        BigDecimal projectedRevenue = currentOrdersInRange.stream()
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

        List<ReturnRequest> currentRefundedReturns = allReturns.stream()
                .filter(this::isActuallyRefunded)
                .filter(request -> isWithinRange(getSafeRefundDate(request), fromDate, toDate))
                .toList();

        List<ReturnRequest> previousRefundedReturns = allReturns.stream()
                .filter(this::isActuallyRefunded)
                .filter(request -> isWithinRange(getSafeRefundDate(request), previousFromDate, previousToDate))
                .toList();

        BigDecimal refundedAmount = sumRefundAmount(currentRefundedReturns);
        BigDecimal previousRefundedAmount = sumRefundAmount(previousRefundedReturns);
        BigDecimal remainingRevenueAfterRefund = totalRevenue.subtract(refundedAmount);

        long pendingOrders = currentOrdersInRange.stream().filter(this::isPendingOrder).count();
        long processingOrders = currentOrdersInRange.stream().filter(this::isProcessingOrder).count();
        long shippingOrders = currentOrdersInRange.stream().filter(this::isShippingOrder).count();
        long cancelledOrders = currentOrdersInRange.stream().filter(this::isCancelledOrder).count();
        long completedOrders = currentCompletedOrders.size();

        return AdminDashboardAnalyticsResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .groupBy(normalizedGroupBy)
                .totalRevenue(totalRevenue)
                .projectedRevenue(projectedRevenue)
                .refundedAmount(refundedAmount)
                .remainingRevenueAfterRefund(remainingRevenueAfterRefund)
                .totalCustomers((long) totalCustomers)
                .newCustomers(newCustomers)
                .soldItems(soldItems)
                .totalOrders((long) currentOrdersInRange.size())
                .completedOrders(completedOrders)
                .pendingOrders(pendingOrders)
                .processingOrders(processingOrders)
                .shippingOrders(shippingOrders)
                .cancelledOrders(cancelledOrders)
                .revenueChangePercent(calculateChangePercent(totalRevenue, previousRevenue))
                .customerChangePercent(calculateChangePercent(BigDecimal.valueOf(newCustomers), BigDecimal.valueOf(previousNewCustomersCount)))
                .soldItemsChangePercent(calculateChangePercent(BigDecimal.valueOf(soldItems), BigDecimal.valueOf(previousSoldItems)))
                .refundedChangePercent(calculateChangePercent(refundedAmount, previousRefundedAmount))
                .timeline(buildTimeline(currentCompletedOrders, currentNewCustomers, currentRefundedReturns, fromDate, toDate, normalizedGroupBy))
                .orderStatusReport(buildOrderStatusReport(currentOrdersInRange))
                .bestSellingProductsByQuantity(buildTopProductsByQuantity(currentCompletedOrders))
                .bestSellingProductsByRevenue(buildTopProductsByRevenue(currentCompletedOrders))
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

        String value = groupBy.trim().toUpperCase();
        if (!value.equals("DAILY") && !value.equals("MONTHLY")) {
            return "DAILY";
        }
        return value;
    }

    private LocalDate getSafeOrderDate(Order order) {
        if (order == null || order.getOrderDate() == null) {
            return LocalDate.now();
        }
        return order.getOrderDate().toLocalDate();
    }

    private LocalDate getSafeUserCreatedDate(UserAccount user) {
        if (user == null || user.getCreatedAt() == null) {
            return LocalDate.now();
        }
        return user.getCreatedAt().toLocalDate();
    }

    private LocalDate getSafeRefundDate(ReturnRequest request) {
        if (request == null) {
            return LocalDate.now();
        }

        if (request.getResolvedAt() != null) {
            return request.getResolvedAt().toLocalDate();
        }

        if (request.getRequestedAt() != null) {
            return request.getRequestedAt().toLocalDate();
        }

        return LocalDate.now();
    }

    private boolean isWithinRange(LocalDate value, LocalDate fromDate, LocalDate toDate) {
        return (value.isEqual(fromDate) || value.isAfter(fromDate))
                && (value.isEqual(toDate) || value.isBefore(toDate));
    }

    private boolean isCompletedOrder(Order order) {
        String status = normalizeStatus(order != null ? order.getStatus() : null);
        return "DELIVERED".equals(status) || "COMPLETED".equals(status);
    }

    private boolean isPendingOrder(Order order) {
        return "PENDING".equals(normalizeStatus(order != null ? order.getStatus() : null));
    }

    private boolean isProcessingOrder(Order order) {
        return "PROCESSING".equals(normalizeStatus(order != null ? order.getStatus() : null));
    }

    private boolean isShippingOrder(Order order) {
        String status = normalizeStatus(order != null ? order.getStatus() : null);
        return "SHIPPING".equals(status) || "SHIPPED".equals(status) || "DELIVERING".equals(status);
    }

    private boolean isCancelledOrder(Order order) {
        String status = normalizeStatus(order != null ? order.getStatus() : null);
        return "CANCELED".equals(status) || "CANCELLED".equals(status);
    }

    private boolean isProjectedOrder(Order order) {
        if (order == null) return false;

        String status = normalizeStatus(order.getStatus());
        if (isCompletedOrder(order) || isCancelledOrder(order)) {
            return false;
        }

        return "PENDING".equals(status)
                || "PROCESSING".equals(status)
                || "SHIPPING".equals(status)
                || "SHIPPED".equals(status)
                || "DELIVERING".equals(status)
                || "PREORDER".equals(status);
    }

    private boolean isActuallyRefunded(ReturnRequest request) {
        if (request == null || request.getStatus() == null) return false;
        return request.getStatus() == ReturnRequest.ReturnStatus.REFUNDED
                || request.getStatus() == ReturnRequest.ReturnStatus.COMPLETED;
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }

    private BigDecimal getOrderAmount(Order order) {
        if (order == null) return BigDecimal.ZERO;
        if (order.getFinalPrice() != null) return order.getFinalPrice();
        if (order.getTotalPrice() != null) return order.getTotalPrice();
        return BigDecimal.ZERO;
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

    private BigDecimal sumRefundAmount(List<ReturnRequest> requests) {
        return requests.stream()
                .map(this::calculateRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Double calculateChangePercent(BigDecimal current, BigDecimal previous) {
        double currentValue = current != null ? current.doubleValue() : 0D;
        double previousValue = previous != null ? previous.doubleValue() : 0D;

        if (previousValue == 0D) {
            if (currentValue == 0D) return 0D;
            return 100D;
        }

        return ((currentValue - previousValue) / previousValue) * 100D;
    }

    private List<OrderStatusReportResponse> buildOrderStatusReport(List<Order> orders) {
        long pending = orders.stream().filter(this::isPendingOrder).count();
        long processing = orders.stream().filter(this::isProcessingOrder).count();
        long shipping = orders.stream().filter(this::isShippingOrder).count();
        long completed = orders.stream().filter(this::isCompletedOrder).count();
        long cancelled = orders.stream().filter(this::isCancelledOrder).count();

        return List.of(
                OrderStatusReportResponse.builder().status("Pending").count(pending).build(),
                OrderStatusReportResponse.builder().status("Processing").count(processing).build(),
                OrderStatusReportResponse.builder().status("Shipping").count(shipping).build(),
                OrderStatusReportResponse.builder().status("Completed").count(completed).build(),
                OrderStatusReportResponse.builder().status("Cancelled").count(cancelled).build()
        );
    }

    private List<DashboardTimePointResponse> buildTimeline(
            List<Order> completedOrders,
            List<UserAccount> newCustomers,
            List<ReturnRequest> refundedRequests,
            LocalDate fromDate,
            LocalDate toDate,
            String groupBy
    ) {
        LinkedHashMap<String, TimePointAccumulator> buckets = initializeBuckets(fromDate, toDate, groupBy);

        for (Order order : completedOrders) {
            String key = buildBucketKey(getSafeOrderDate(order), groupBy);
            TimePointAccumulator bucket = buckets.get(key);
            if (bucket == null) continue;

            bucket.revenue = bucket.revenue.add(getOrderAmount(order));

            long soldQuantity = getSafeOrderItems(order).stream()
                    .mapToLong(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                    .sum();

            bucket.soldItems += soldQuantity;
        }

        for (UserAccount customer : newCustomers) {
            String key = buildBucketKey(getSafeUserCreatedDate(customer), groupBy);
            TimePointAccumulator bucket = buckets.get(key);
            if (bucket == null) continue;

            bucket.customerRegistrations += 1;
        }

        for (ReturnRequest request : refundedRequests) {
            String key = buildBucketKey(getSafeRefundDate(request), groupBy);
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
        Map<String, ProductAccumulator> map = buildProductMap(completedOrders);

        return map.values().stream()
                .sorted(Comparator.comparing(ProductAccumulator::getQuantitySold).reversed())
                .limit(8)
                .map(item -> ProductReportResponse.builder()
                        .productName(item.productName)
                        .quantitySold(item.quantitySold)
                        .revenue(item.revenue)
                        .build())
                .toList();
    }

    private List<ProductReportResponse> buildTopProductsByRevenue(List<Order> completedOrders) {
        Map<String, ProductAccumulator> map = buildProductMap(completedOrders);

        return map.values().stream()
                .sorted(Comparator.comparing(ProductAccumulator::getRevenue).reversed())
                .limit(8)
                .map(item -> ProductReportResponse.builder()
                        .productName(item.productName)
                        .quantitySold(item.quantitySold)
                        .revenue(item.revenue)
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

    private String resolveCustomerName(Order order, UserAccount user) {
        if (user != null && user.getName() != null && !user.getName().isBlank()) {
            return user.getName().trim();
        }
        if (order.getFullName() != null && !order.getFullName().isBlank()) {
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

    private Map<String, ProductAccumulator> buildProductMap(List<Order> completedOrders) {
        Map<String, ProductAccumulator> map = new HashMap<>();

        for (Order order : completedOrders) {
            for (OrderItem item : getSafeOrderItems(order)) {
                String productName = item.getProductName();
                if (productName == null || productName.isBlank()) {
                    productName = "Unknown Product";
                }

                ProductAccumulator accumulator = map.computeIfAbsent(productName, ProductAccumulator::new);

                long quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal lensPrice = item.getLensPrice() != null ? item.getLensPrice() : BigDecimal.ZERO;
                BigDecimal revenue = unitPrice.add(lensPrice).multiply(BigDecimal.valueOf(quantity));

                accumulator.quantitySold += quantity;
                accumulator.revenue = accumulator.revenue.add(revenue);
            }
        }

        return map;
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