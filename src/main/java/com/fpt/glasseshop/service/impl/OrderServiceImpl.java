package com.fpt.glasseshop.service.impl;

import com.fpt.glasseshop.entity.*;
import com.fpt.glasseshop.entity.dto.*;
import com.fpt.glasseshop.exception.ResourceNotFoundException;
import com.fpt.glasseshop.repository.CartRepository;
import com.fpt.glasseshop.repository.OrderRepository;
import com.fpt.glasseshop.repository.PaymentRepository;
import com.fpt.glasseshop.repository.ProductVariantRepository;
import com.fpt.glasseshop.service.NotificationService;
import com.fpt.glasseshop.service.OrderItemService;
import com.fpt.glasseshop.service.OrderService;
import com.fpt.glasseshop.service.VNPayRefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemService orderItemService;
    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;
    private final NotificationService notificationService;
    private final PaymentRepository paymentRepository;
    private final VNPayRefundService vnPayRefundService;

    @Override
    public Order saveOrder(Order order) {
        Order savedOrder = orderRepository.save(order);
        if (order.getOrderItems() != null) {
            order.getOrderItems().forEach(item -> {
                item.setOrder(savedOrder);
                orderItemService.saveOrderItem(item);
            });
        }
        return savedOrder;
    }

    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserUserId(userId);
    }

    @Override
    public List<OrderDTO> getOrdersDTOByUserId(Long userId) {
        return getOrdersByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getAllOrdersDTO() {
        return orderRepository.findAllByOrderByOrderDateDesc().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    @Override
    public Optional<OrderDTO> getOrderDTOById(Long orderId) {
        return getOrderById(orderId).map(this::convertToDTO);
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (newStatus == null || newStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("Order status is required");
        }

        List<String> validStatuses = Arrays.asList(
                "PENDING", "PROCESSING", "DELIVERING", "DELIVERED",
                "CANCELED", "CANCELLED", "SHIPPED", "PREORDER", "COMPLETED", "REFUNDED"
        );

        String targetStatus = newStatus.trim().toUpperCase();
        if ("REFUND".equals(targetStatus)) {
            targetStatus = "REFUNDED";
        }
        if (!validStatuses.contains(targetStatus)) {
            throw new IllegalArgumentException("Invalid order status: " + targetStatus);
        }

        if (("PENDING".equalsIgnoreCase(order.getStatus()) || "PREORDER".equalsIgnoreCase(order.getStatus()))
                && "PROCESSING".equals(targetStatus)) {
            order.setStockReadyAt(LocalDateTime.now());
        }

        if ("PROCESSING".equalsIgnoreCase(order.getStatus())
                && ("SHIPPED".equals(targetStatus) || "DELIVERING".equals(targetStatus))) {
            boolean hasUnapprovedPrescription = order.getOrderItems().stream()
                    .anyMatch(item -> requiresPrescriptionApproval(item)
                            && !Boolean.TRUE.equals(item.getPrescription().getStatus()));
            if (hasUnapprovedPrescription) {
                throw new IllegalStateException("Cannot ship order: all prescriptions must be approved first.");
            }
        }

        order.setStatus(targetStatus);

        if ("DELIVERED".equalsIgnoreCase(targetStatus) || "COMPLETED".equalsIgnoreCase(targetStatus)) {
            boolean isPartialPreorder = "PARTIAL".equalsIgnoreCase(order.getDepositType());

            // Chỉ auto PAID cho đơn thường COD
            if (!isPartialPreorder && "COD".equalsIgnoreCase(order.getPaymentMethod())) {
                order.setPaymentStatus("PAID");
            }

            // Đơn preorder 50% thì KHÔNG auto PAID_FULL ở đây
            if (order.getDeliveredAt() == null) {
                order.setDeliveredAt(LocalDateTime.now());
            }
        }

        if ("CANCELED".equals(targetStatus) || "CANCELLED".equals(targetStatus)) {
            restoreStock(order);

            if ("UNPAID".equalsIgnoreCase(order.getPaymentStatus())) {
                order.setPaymentStatus("CANCELLED");
            }

            if ("VNPAY".equalsIgnoreCase(order.getPaymentMethod())
                    && ("PAID".equalsIgnoreCase(order.getPaymentStatus())
                    || "PAID_DEPOSIT".equalsIgnoreCase(order.getPaymentStatus())
                    || "PAID_FULL".equalsIgnoreCase(order.getPaymentStatus()))) {
                if (order.getRefundStatus() == null || order.getRefundStatus().isBlank()) {
                    order.setRefundStatus("WAITING_REFUND");
                }
                if (order.getRefundRequestedAt() == null) {
                    order.setRefundRequestedAt(LocalDateTime.now());
                }
            }
        }

        notificationService.createNotification(
                order.getUser(),
                "Order Status Updated",
                "Your order " + order.getOrderCode() + " is now " + targetStatus,
                "ORDER",
                order.getOrderId()
        );

        return convertToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public VNPayRefundResult refundVnpayForCancelledOrder(Long orderId, String requesterEmail, String reason) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (requesterEmail == null || requesterEmail.isBlank()
                || order.getUser() == null
                || order.getUser().getEmail() == null
                || !order.getUser().getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new org.springframework.security.access.AccessDeniedException("You are not authorized to refund this order");
        }

        String paymentMethod = order.getPaymentMethod() != null ? order.getPaymentMethod().trim() : "";
        if (!"VNPAY".equalsIgnoreCase(paymentMethod)) {
            throw new IllegalArgumentException("Order is not paid via VNPay");
        }

        String paymentStatus = order.getPaymentStatus() != null ? order.getPaymentStatus().trim() : "";
        if (!"PAID".equalsIgnoreCase(paymentStatus)
            && !"PAID_DEPOSIT".equalsIgnoreCase(paymentStatus)
            && !"PAID_FULL".equalsIgnoreCase(paymentStatus)) {
            throw new IllegalArgumentException("Order is not in a paid state");
        }

        String status = order.getStatus() != null ? order.getStatus().trim().toUpperCase() : "";
        boolean cancellableStatus = Arrays.asList(
                "PENDING",
                "PREORDER",
                "PROCESSING",
                "CANCELED",
                "CANCELLED"
        ).contains(status);

        if (!cancellableStatus) {
            throw new IllegalArgumentException("Order cannot be refunded at current status: " + status);
        }

        if (paymentRepository.existsByOrderOrderIdAndPaymentMethodIgnoreCaseAndAmountLessThan(
                orderId,
                "VNPAY",
                BigDecimal.ZERO
        )) {
            return VNPayRefundResult.builder()
                    .success(true)
                    .refundStatus("REFUNDED")
                    .message("Order already has a VNPay refund recorded")
                    .transactionReference(null)
                    .build();
        }

        if ("PENDING".equals(status) || "PREORDER".equals(status) || "PROCESSING".equals(status)) {
            updateOrderStatus(orderId, "CANCELLED");
            order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        }

        Payment paidPayment = paymentRepository
                .findTopByOrderOrderIdAndPaymentMethodIgnoreCaseAndAmountGreaterThanAndStatusOrderByPaidAtDesc(
                        orderId,
                        "VNPAY",
                        BigDecimal.ZERO,
                        "SUCCESS"
                );

        if (paidPayment == null || paidPayment.getTransactionReference() == null || paidPayment.getTransactionReference().isBlank()) {
            order.setRefundStatus("WAITING_REFUND");
            order.setRefundNote(reason);
            orderRepository.save(order);

            return VNPayRefundResult.builder()
                    .success(false)
                    .refundStatus("WAITING_REFUND")
                    .message("Missing VNPay transaction reference; cannot process auto-refund")
                    .build();
        }

        order.setRefundStatus("PENDING");
        if (order.getRefundRequestedAt() == null) {
            order.setRefundRequestedAt(LocalDateTime.now());
        }
        if (reason != null && !reason.isBlank()) {
            order.setRefundNote(reason);
        }
        orderRepository.save(order);

        VNPayRefundResult refundResult = vnPayRefundService.refund(order, paidPayment, requesterEmail, reason);

        if (refundResult.isSuccess()) {
            Payment refundPayment = Payment.builder()
                    .order(order)
                    .paymentMethod("VNPAY")
                    .amount(paidPayment.getAmount() != null ? paidPayment.getAmount().negate() : BigDecimal.ZERO)
                    .status("REFUNDED")
                    .transactionReference(paidPayment.getTransactionReference())
                    .paidAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(refundPayment);

            order.setRefundStatus("REFUNDED");
            order.setRefundProcessedAt(LocalDateTime.now());
            order.setStatus("REFUNDED");
            orderRepository.save(order);
        } else {
            order.setRefundStatus("WAITING_REFUND");
            orderRepository.save(order);
        }

        refundResult.setRefundStatus(order.getRefundStatus());

        return refundResult;
    }

    @Override
    @Transactional
    public OrderDTO requestRefundForCancelledOrder(Long orderId, String requesterEmail, OrderRefundRequestDTO dto) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (requesterEmail == null || requesterEmail.isBlank()
                || order.getUser() == null
                || order.getUser().getEmail() == null
                || !order.getUser().getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new org.springframework.security.access.AccessDeniedException("You are not authorized to request a refund for this order");
        }

        String paymentMethod = order.getPaymentMethod() != null ? order.getPaymentMethod().trim() : "";
        if (!"VNPAY".equalsIgnoreCase(paymentMethod)) {
            throw new IllegalArgumentException("Order is not paid via VNPay");
        }

        String paymentStatus = order.getPaymentStatus() != null ? order.getPaymentStatus().trim() : "";
        if (!"PAID".equalsIgnoreCase(paymentStatus)
            && !"PAID_DEPOSIT".equalsIgnoreCase(paymentStatus)
            && !"PAID_FULL".equalsIgnoreCase(paymentStatus)) {
            throw new IllegalArgumentException("Order is not in a paid state");
        }

        String status = order.getStatus() != null ? order.getStatus().trim().toUpperCase() : "";
        if (!Arrays.asList("CANCELED", "CANCELLED", "REFUND", "REFUNDED").contains(status)) {
            throw new IllegalArgumentException("Order must be cancelled before requesting a refund");
        }

        if (dto == null) {
            throw new IllegalArgumentException("Refund request payload is required");
        }

        if (dto.getBankName() == null || dto.getBankName().isBlank()) {
            throw new IllegalArgumentException("Bank name is required");
        }

        if (dto.getBankAccountNumber() == null || dto.getBankAccountNumber().isBlank()) {
            throw new IllegalArgumentException("Bank account number is required");
        }

        if (dto.getBankAccountHolder() == null || dto.getBankAccountHolder().isBlank()) {
            throw new IllegalArgumentException("Bank account holder is required");
        }

        order.setRefundStatus("PENDING");
        if (order.getRefundRequestedAt() == null) {
            order.setRefundRequestedAt(LocalDateTime.now());
        }

        order.setRefundBankName(dto.getBankName());
        order.setRefundBankAccountNumber(dto.getBankAccountNumber());
        order.setRefundBankAccountHolder(dto.getBankAccountHolder());
        if (dto.getNote() != null && !dto.getNote().isBlank()) {
            order.setRefundNote(dto.getNote());
        }

        return convertToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDTO requestVnpayRefundForCancelledOrder(Long orderId, String requesterEmail, String reason) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (requesterEmail == null || requesterEmail.isBlank()
                || order.getUser() == null
                || order.getUser().getEmail() == null
                || !order.getUser().getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new org.springframework.security.access.AccessDeniedException("You are not authorized to request a refund for this order");
        }

        String paymentMethod = order.getPaymentMethod() != null ? order.getPaymentMethod().trim() : "";
        if (!"VNPAY".equalsIgnoreCase(paymentMethod)) {
            throw new IllegalArgumentException("Order is not paid via VNPay");
        }

        String paymentStatus = order.getPaymentStatus() != null ? order.getPaymentStatus().trim() : "";
        if (!"PAID".equalsIgnoreCase(paymentStatus) && !"PAID_FULL".equalsIgnoreCase(paymentStatus)) {
            throw new IllegalArgumentException("Order is not in a paid state");
        }

        String status = order.getStatus() != null ? order.getStatus().trim().toUpperCase() : "";
        boolean cancellableStatus = Arrays.asList(
                "PENDING",
                "PREORDER",
                "PROCESSING",
                "CANCELED",
                "CANCELLED"
        ).contains(status);

        if (!cancellableStatus) {
            throw new IllegalArgumentException("Order cannot be refunded at current status: " + status);
        }

        if ("PENDING".equals(status) || "PREORDER".equals(status) || "PROCESSING".equals(status)) {
            updateOrderStatus(orderId, "CANCELLED");
            order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        }

        order.setRefundStatus("PENDING");
        if (order.getRefundRequestedAt() == null) {
            order.setRefundRequestedAt(LocalDateTime.now());
        }
        if (reason != null && !reason.isBlank()) {
            order.setRefundNote(reason);
        }

        return convertToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDTO confirmRefundedForCancelledOrder(Long orderId, String confirmerEmail, RefundProcessDTO dto) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        String refundStatus = order.getRefundStatus() != null ? order.getRefundStatus().trim().toUpperCase() : "";
        if (!"PENDING".equals(refundStatus)) {
            throw new IllegalArgumentException("Refund can only be confirmed when status is PENDING");
        }

        String note = dto != null ? dto.getNote() : null;
        if (note != null && !note.isBlank()) {
            order.setRefundNote(note);
        }

        boolean hasManualBankInfo = order.getRefundBankAccountNumber() != null
                && !order.getRefundBankAccountNumber().isBlank();

        if (!hasManualBankInfo && order.getPaymentMethod() != null
                && "VNPAY".equalsIgnoreCase(order.getPaymentMethod().trim())) {

            if (paymentRepository.existsByOrderOrderIdAndPaymentMethodIgnoreCaseAndAmountLessThan(
                    orderId,
                    "VNPAY",
                    BigDecimal.ZERO
            )) {
                order.setRefundStatus("REFUNDED");
                order.setRefundProcessedAt(LocalDateTime.now());
                order.setStatus("REFUNDED");
                return convertToDTO(orderRepository.save(order));
            }

            Payment paidPayment = paymentRepository
                    .findTopByOrderOrderIdAndPaymentMethodIgnoreCaseAndAmountGreaterThanAndStatusOrderByPaidAtDesc(
                            orderId,
                            "VNPAY",
                            BigDecimal.ZERO,
                            "SUCCESS"
                    );

            if (paidPayment == null || paidPayment.getTransactionReference() == null || paidPayment.getTransactionReference().isBlank()) {
                throw new IllegalArgumentException("Missing VNPay transaction reference; cannot process auto-refund");
            }

            VNPayRefundResult refundResult = vnPayRefundService.refund(order, paidPayment, confirmerEmail, note);

            if (!refundResult.isSuccess()) {
                String code = refundResult.getResponseCode();
                String reqId = refundResult.getRequestId();
                String msg = refundResult.getMessage() != null ? refundResult.getMessage() : "unknown error";
                String raw = refundResult.getRawResponse();
                String rawSnippet = null;
                if (raw != null && !raw.isBlank()) {
                    String trimmed = raw.trim().replaceAll("\\s+", " ");
                    rawSnippet = trimmed.length() > 300 ? trimmed.substring(0, 300) + "..." : trimmed;
                }
                throw new IllegalStateException(
                        "VNPay refund failed" +
                                (code != null ? " (code=" + code + ")" : "") +
                                (reqId != null ? " (requestId=" + reqId + ")" : "") +
                                ": " + msg +
                                (rawSnippet != null ? " | raw=" + rawSnippet : "")
                );
            }

            Payment refundPayment = Payment.builder()
                    .order(order)
                    .paymentMethod("VNPAY")
                    .amount(paidPayment.getAmount() != null ? paidPayment.getAmount().negate() : BigDecimal.ZERO)
                    .status("REFUNDED")
                    .transactionReference(paidPayment.getTransactionReference())
                    .paidAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(refundPayment);

            order.setRefundStatus("REFUNDED");
            order.setRefundProcessedAt(LocalDateTime.now());
            order.setStatus("REFUNDED");
            return convertToDTO(orderRepository.save(order));
        }

        order.setRefundStatus("REFUNDED");
        order.setRefundProcessedAt(LocalDateTime.now());
        order.setStatus("REFUNDED");
        return convertToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDTO updatePaymentOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        List<String> validStatuses = Arrays.asList("UNPAID", "PAID", "PAID_DEPOSIT", "PAID_FULL", "FAILED", "CANCELLED");
        String targetStatus = newStatus == null ? "" : newStatus.toUpperCase().trim();
        String currentStatus = order.getPaymentStatus() == null
                ? "UNPAID"
                : order.getPaymentStatus().trim().toUpperCase();

        if (!validStatuses.contains(targetStatus)) {
            throw new IllegalArgumentException("Invalid payment status: " + targetStatus);
        }

        boolean isPartialOrder = "PARTIAL".equalsIgnoreCase(order.getDepositType());
        String currentOrderStatus = order.getStatus() == null ? "" : order.getStatus().trim().toUpperCase();

        if ("PAID_DEPOSIT".equals(targetStatus)) {
            if (!isPartialOrder) {
                throw new IllegalStateException("PAID_DEPOSIT is only allowed for PARTIAL orders");
            }

            if (!"PAID_DEPOSIT".equals(currentStatus) && !"PAID_FULL".equals(currentStatus)) {
                order.setPaymentStatus("PAID_DEPOSIT");
            }

            order.setRemainingPaymentStage("UNPAID");
        } else
        if ("PAID".equals(targetStatus)) {
            if (!"PAID".equals(currentStatus) && !"PAID_FULL".equals(currentStatus)) {
                order.setPaymentStatus("PAID");
            }
            if (isPartialOrder) {
                order.setRemainingPaymentStage("UNPAID"); // 🔥 luôn reset
            }
        } else if ("PAID_FULL".equals(targetStatus)) {
                if (isPartialOrder) {
                    if (!"COMPLETED".equals(currentOrderStatus)
                            && !"DELIVERED".equals(currentOrderStatus)) {
                        throw new IllegalStateException(
                                "Order must be DELIVERED or COMPLETED before full payment"
                        );
                    }

                    if (order.getRemainingPaymentMethod() == null
                            || order.getRemainingPaymentMethod().isBlank()) {

                        if ("PENDING_CONFIRMATION".equalsIgnoreCase(order.getRemainingPaymentStage())) {
                            order.setRemainingPaymentMethod("COD");
                        } else if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
                            // Backward-compatible fallback for older partial COD orders
                            // that never persisted remainingPaymentMethod explicitly.
                            order.setRemainingPaymentMethod("COD");
                        } else {
                            throw new IllegalStateException("Remaining payment method is not selected");
                        }
                    }
                }

                order.setPaymentStatus("PAID_FULL");

                if (isPartialOrder) {
                    order.setRemainingPaymentStage("PAID");
                }
        } else {
            order.setPaymentStatus(targetStatus);
        }

        if (("PAID".equals(order.getPaymentStatus()) || "PAID_FULL".equals(order.getPaymentStatus()))
                && ("PENDING".equalsIgnoreCase(order.getStatus()) || "PREORDER".equalsIgnoreCase(order.getStatus()))) {
            String paymentMethod = order.getPaymentMethod() != null ? order.getPaymentMethod().trim() : "";
            boolean isPreorder = "PREORDER".equalsIgnoreCase(order.getStatus())
                    || "PARTIAL".equalsIgnoreCase(order.getDepositType());

            if (!"VNPAY".equalsIgnoreCase(paymentMethod) && !isPreorder) {
                order.setStatus("PROCESSING");
            }
        }

        if ("FAILED".equals(targetStatus)
                && ("PENDING".equalsIgnoreCase(order.getStatus()) || "PREORDER".equalsIgnoreCase(order.getStatus()))) {
            order.setStatus("CANCELLED");
            restoreStock(order);
        }

        if ("CANCELLED".equals(targetStatus)) {
            order.setStatus("CANCELLED");
            restoreStock(order);
        }

        return convertToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDTO updatePaymentMethod(Long orderId, String newMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        boolean isPartialOrder = "PARTIAL".equalsIgnoreCase(order.getDepositType());
        String paymentStatus = order.getPaymentStatus() == null
                ? ""
                : order.getPaymentStatus().trim().toUpperCase();
        String remainingStage = order.getRemainingPaymentStage() == null
                ? ""
                : order.getRemainingPaymentStage().trim().toUpperCase();
        boolean isCompleted = "COMPLETED".equalsIgnoreCase(order.getStatus())
                || "DELIVERED".equalsIgnoreCase(order.getStatus());

        // For PARTIAL orders: allow choosing remaining payment method after deposit is paid.
        // This must NOT overwrite the deposit payment method.
        if (isPartialOrder
                && "PAID_DEPOSIT".equals(paymentStatus)
                && !"PAID".equalsIgnoreCase(remainingStage)
                && !"PAID_FULL".equals(paymentStatus)) {

            order.setRemainingPaymentMethod(newMethod);

            if ("COD".equalsIgnoreCase(newMethod)) {
                order.setRemainingPaymentStage("PENDING_CONFIRMATION");
            } else {
                order.setRemainingPaymentStage("UNPAID");
            }

            return convertToDTO(orderRepository.save(order));
        }

        if (isPartialOrder && isCompleted && !"PAID_FULL".equalsIgnoreCase(order.getPaymentStatus())) {
            order.setRemainingPaymentMethod(newMethod);

            if ("COD".equalsIgnoreCase(newMethod)) {
                order.setRemainingPaymentStage("PENDING_CONFIRMATION");
            } else {
                order.setRemainingPaymentStage("UNPAID");
            }

            return convertToDTO(orderRepository.save(order));
        }

        order.setPaymentMethod(newMethod);
        return convertToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDTO cancelPendingPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!"UNPAID".equalsIgnoreCase(order.getPaymentStatus())) {
            return convertToDTO(order);
        }

        if (!"PENDING".equalsIgnoreCase(order.getStatus()) && !"PREORDER".equalsIgnoreCase(order.getStatus())) {
            return convertToDTO(order);
        }

        order.setStatus("CANCELLED");
        order.setPaymentStatus("CANCELLED");
        restoreStock(order);

        notificationService.createNotification(
                order.getUser(),
                "Payment Cancelled",
                "Your payment for order " + order.getOrderCode() + " was cancelled.",
                "ORDER",
                order.getOrderId()
        );

        return convertToDTO(orderRepository.save(order));
    }

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void expirePendingVnpayOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        List<Order> expiredOrders = orderRepository.findExpiredPendingVnpayOrders(cutoff);

        for (Order order : expiredOrders) {
            order.setStatus("CANCELLED");
            order.setPaymentStatus("CANCELLED");
            restoreStock(order);
            orderRepository.save(order);
        }
    }

    @Override
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Order not found with id: " + id);
        }
        orderRepository.deleteById(id);
    }

    @Override
    @Transactional
    public OrderDTO createOrderFromCart(UserAccount user, CreateOrderRequest request) {
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().trim().isEmpty()) {
            Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existingOrder.isPresent()) {
                return convertToDTO(existingOrder.get());
            }
        }

        Cart cart = cartRepository.findByUserUserIdForCheckout(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + user.getUserId()));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot create order from an empty cart");
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal preorderItemsTotal = BigDecimal.ZERO;
        BigDecimal inStockItemsTotal = BigDecimal.ZERO;
        BigDecimal shippingFee = request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.ZERO;
        BigDecimal voucherDiscount = request.getVoucherDiscount() != null ? request.getVoucherDiscount() : BigDecimal.ZERO;

        String orderCode = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String initialStatus = Boolean.TRUE.equals(request.getIsPreorder()) ? "PREORDER" : "PENDING";

        Order order = Order.builder()
                .user(user)
                .orderCode(orderCode)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .note(request.getNote())
                .paymentMethod(request.getPaymentMethod())
                .shippingFee(shippingFee)
                .voucherDiscount(voucherDiscount)
                .idempotencyKey(request.getIdempotencyKey())
                .status(initialStatus)
                .paymentStatus("UNPAID")
                .depositType(request.getDepositType())
                .depositPaymentMethod(request.getPaymentMethod())
                .remainingPaymentMethod(null)
                .remainingPaymentStage("UNPAID")
                .orderDate(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .build();

        for (CartItem cartItem : cart.getItems()) {
            if (cartItem.getQuantity() == null || cartItem.getQuantity() <= 0) {
                throw new IllegalArgumentException("Invalid quantity for cart item");
            }

            boolean isPreorderItem = Boolean.TRUE.equals(cartItem.getIsPreorder());

            if (!isPreorderItem) {
                int updatedRows = productVariantRepository.decreaseStock(
                        cartItem.getVariant().getVariantId(),
                        cartItem.getQuantity()
                );
                if (updatedRows == 0) {
                    throw new IllegalArgumentException("Insufficient stock for product");
                }
            }

            BigDecimal unitPrice = cartItem.getPrice() != null ? cartItem.getPrice() : BigDecimal.ZERO;
            BigDecimal lensPrice = (cartItem.getLensOption() != null && cartItem.getLensOption().getPrice() != null)
                    ? cartItem.getLensOption().getPrice()
                    : BigDecimal.ZERO;

            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalPrice = totalPrice.add(subtotal);

            if (isPreorderItem) {
                preorderItemsTotal = preorderItemsTotal.add(subtotal);
            } else {
                inStockItemsTotal = inStockItemsTotal.add(subtotal);
            }

            for (int i = 0; i < cartItem.getQuantity(); i++) {
                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .variant(cartItem.getVariant())
                        .variantId(cartItem.getVariant() != null ? cartItem.getVariant().getVariantId() : null)
                        .productId(cartItem.getVariant() != null && cartItem.getVariant().getProduct() != null
                                ? cartItem.getVariant().getProduct().getProductId()
                                : cartItem.getProductId())
                        .productName(cartItem.getVariant() != null && cartItem.getVariant().getProduct() != null
                                ? cartItem.getVariant().getProduct().getName()
                                : cartItem.getProductName())
                        .variantColor(cartItem.getVariant() != null ? cartItem.getVariant().getColor() : null)
                        .variantSize(cartItem.getVariant() != null ? cartItem.getVariant().getFrameSize() : null)
                        .imageUrl(cartItem.getVariant() != null ? cartItem.getVariant().getImageUrl() : null)
                        .lensOption(cartItem.getLensOption())
                        .lensOptionId(cartItem.getLensOption() != null ? cartItem.getLensOption().getLensOptionId() : null)
                        .lensType(cartItem.getLensOption() != null ? cartItem.getLensOption().getType() : null)
                        .lensPrice(lensPrice)
                        .lensCoating(cartItem.getLensOption() != null ? cartItem.getLensOption().getCoating() : null)
                        .quantity(1)
                        .unitPrice(unitPrice)
                        .isPreorder(isPreorderItem)
                        .fulfillmentType(cartItem.getPrescription() != null
                                ? "PRESCRIPTION"
                                : (isPreorderItem ? "PRE_ORDER" : "IN_STOCK"))
                        .sphLeft(cartItem.getPrescription() != null ? cartItem.getPrescription().getSphLeft() : null)
                        .sphRight(cartItem.getPrescription() != null ? cartItem.getPrescription().getSphRight() : null)
                        .cylLeft(cartItem.getPrescription() != null ? cartItem.getPrescription().getCylLeft() : null)
                        .cylRight(cartItem.getPrescription() != null ? cartItem.getPrescription().getCylRight() : null)
                        .axisLeft(cartItem.getPrescription() != null ? cartItem.getPrescription().getAxisLeft() : null)
                        .axisRight(cartItem.getPrescription() != null ? cartItem.getPrescription().getAxisRight() : null)
                        .addLeft(cartItem.getPrescription() != null ? cartItem.getPrescription().getAddLeft() : null)
                        .addRight(cartItem.getPrescription() != null ? cartItem.getPrescription().getAddRight() : null)
                        .build();

                if (cartItem.getPrescription() != null) {
                    Prescription cartP = cartItem.getPrescription();
                    Prescription p = Prescription.builder()
                            .orderItem(orderItem)
                            .cartItem(null)
                            .sphLeft(cartP.getSphLeft())
                            .sphRight(cartP.getSphRight())
                            .cylLeft(cartP.getCylLeft())
                            .cylRight(cartP.getCylRight())
                            .axisLeft(cartP.getAxisLeft())
                            .axisRight(cartP.getAxisRight())
                            .addLeft(cartP.getAddLeft())
                            .addRight(cartP.getAddRight())
                            .doctorName(cartP.getDoctorName())
                            .expirationDate(cartP.getExpirationDate())
                            .status(cartP.getStatus() != null ? cartP.getStatus() : false)
                            .build();
                    orderItem.setPrescription(p);
                }

                order.getOrderItems().add(orderItem);
            }
        }

        order.setTotalPrice(totalPrice);
        BigDecimal finalTotal = totalPrice.add(shippingFee).subtract(voucherDiscount);
        order.setFinalPrice(finalTotal);

        if ("PARTIAL".equalsIgnoreCase(request.getDepositType())) {
            if (preorderItemsTotal.compareTo(BigDecimal.ZERO) <= 0) {
                order.setDepositAmount(finalTotal.max(BigDecimal.ZERO));
            } else {
                BigDecimal halfPreorder = preorderItemsTotal
                        .divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP)
                        .max(BigDecimal.ZERO);

                BigDecimal deposit = inStockItemsTotal
                        .add(shippingFee)
                        .subtract(voucherDiscount)
                        .add(halfPreorder);

                if (deposit.compareTo(BigDecimal.ZERO) < 0) {
                    deposit = BigDecimal.ZERO;
                }

                if (deposit.compareTo(finalTotal) > 0) {
                    deposit = finalTotal;
                }

                order.setDepositAmount(deposit);
            }
        } else {
            order.setDepositAmount(finalTotal);
        }

        Order savedOrder = orderRepository.save(order);

        notificationService.notifyAdmins(
                "New Order Received",
                "A new order " + savedOrder.getOrderCode() + " has been placed by " + savedOrder.getFullName(),
                "ORDER",
                savedOrder.getOrderId()
        );

        return convertToDTO(savedOrder);
    }

    @Override
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemService.getOrderItemsByOrderId(orderId);
    }

    @Override
    @Transactional
    public OrderDTO approvePreorder(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            throw new IllegalStateException("Order has no items");
        }

        String status = order.getStatus() != null ? order.getStatus().trim().toUpperCase() : "";
        boolean alreadyApproved = order.getStockReadyAt() != null && "PROCESSING".equals(status);
        if (alreadyApproved) {
            return convertToDTO(order);
        }

        boolean hasPreorderItems = order.getOrderItems().stream()
                .anyMatch(item -> Boolean.TRUE.equals(item.getIsPreorder()));

        if (!hasPreorderItems) {
            throw new IllegalStateException("Order does not contain preorder items");
        }

        // Deduct stock for preorder items.
        // Use conditional UPDATE in repository to prevent negative stock.
        for (OrderItem item : order.getOrderItems()) {
            if (!Boolean.TRUE.equals(item.getIsPreorder())) {
                continue;
            }

            Long variantId = item.getVariantId();
            Integer quantity = item.getQuantity();

            if (variantId == null || quantity == null || quantity <= 0) {
                throw new IllegalStateException("Invalid preorder item stock data");
            }

            int updated = productVariantRepository.decreaseStock(variantId, quantity);
            if (updated <= 0) {
                String itemName = item.getProductName() != null ? item.getProductName() : "variant " + variantId;
                throw new IllegalStateException(
                        "Insufficient stock to approve preorder for: " + itemName
                );
            }

            // Mark item as no longer a preorder so it disappears from Pre-Order list.
            item.setIsPreorder(false);
        }

        order.setStockReadyAt(LocalDateTime.now());
        order.setStatus("PROCESSING");

        notificationService.createNotification(
                order.getUser(),
                "Preorder Approved",
                "Your preorder " + order.getOrderCode() + " is now processing",
                "ORDER",
                order.getOrderId()
        );

        return convertToDTO(orderRepository.save(order));
    }

    private void restoreStock(Order order) {
        if (order.getOrderItems() == null) return;

        boolean preorderStockWasDeducted = order.getStockReadyAt() != null;

        for (OrderItem item : order.getOrderItems()) {
            boolean isPreorderItem = Boolean.TRUE.equals(item.getIsPreorder());

            if (isPreorderItem && !preorderStockWasDeducted) {
                continue;
            }

            if (item.getVariantId() != null && item.getQuantity() != null) {
                productVariantRepository.decreaseStock(item.getVariantId(), -item.getQuantity());
            }
        }
    }

    private OrderDTO convertToDTO(Order order) {
        return OrderDTO.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .userId(order.getUser() != null ? order.getUser().getUserId() : null)
                .userName(order.getUser() != null ? order.getUser().getName() : null)
                .userEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .fullName(order.getFullName())
                .phone(order.getPhone())
                .address(order.getAddress())
                .note(order.getNote())
                .shippingFee(order.getShippingFee())
                .voucherDiscount(order.getVoucherDiscount())
                .finalPrice(order.getFinalPrice())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .depositAmount(order.getDepositAmount())
                .depositType(order.getDepositType())
                .depositPaymentMethod(order.getDepositPaymentMethod())
                .remainingPaymentMethod(order.getRemainingPaymentMethod())
                .remainingPaymentStage(order.getRemainingPaymentStage())
                .stockReadyAt(order.getStockReadyAt())
                .refundStatus(order.getRefundStatus())
                .refundRequestedAt(order.getRefundRequestedAt())
                .refundProcessedAt(order.getRefundProcessedAt())
                .refundBankAccountNumber(order.getRefundBankAccountNumber())
                .refundBankName(order.getRefundBankName())
                .refundBankAccountHolder(order.getRefundBankAccountHolder())
                .refundNote(order.getRefundNote())
                .orderItems(order.getOrderItems() != null
                        ? order.getOrderItems().stream().map(this::mapToItemDTO).collect(Collectors.toList())
                        : null)
                .totalItems(order.getOrderItems() != null ? order.getOrderItems().size() : 0)
                .build();
    }

    private OrderItemDTO mapToItemDTO(OrderItem item) {
        if (item == null) return null;

        return OrderItemDTO.builder()
                .orderItemId(item.getOrderItemId())
                .variantId(item.getVariantId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .variantColor(item.getVariantColor())
                .variantSize(item.getVariantSize())
                .imageUrl(item.getImageUrl())
                .lensType(item.getLensType())
                .lensPrice(item.getLensPrice())
                .lensCoating(item.getLensCoating())
                .lensOptionId(item.getLensOptionId())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .isPreorder(item.getIsPreorder())
                .fulfillmentType(item.getFulfillmentType())
                .sphLeft(item.getSphLeft())
                .sphRight(item.getSphRight())
                .cylLeft(item.getCylLeft())
                .cylRight(item.getCylRight())
                .axisLeft(item.getAxisLeft())
                .axisRight(item.getAxisRight())
                .addLeft(item.getAddLeft())
                .addRight(item.getAddRight())
                .prescription(item.getPrescription() != null ? mapPrescriptionDTO(item.getPrescription()) : null)
                .build();
    }

    private PrescriptionDTO mapPrescriptionDTO(Prescription p) {
        if (p == null) return null;

        return PrescriptionDTO.builder()
                .prescriptionId(p.getPrescriptionId())
                .orderItemId(p.getOrderItem() != null ? p.getOrderItem().getOrderItemId() : null)
                .sphLeft(p.getSphLeft())
                .sphRight(p.getSphRight())
                .cylLeft(p.getCylLeft())
                .cylRight(p.getCylRight())
                .axisLeft(p.getAxisLeft())
                .axisRight(p.getAxisRight())
                .addLeft(p.getAddLeft())
                .addRight(p.getAddRight())
                .doctorName(p.getDoctorName())
                .expirationDate(p.getExpirationDate())
                .status(p.getStatus())
                .adminNote(p.getAdminNote())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private boolean requiresPrescriptionApproval(OrderItem item) {
        Prescription prescription = item.getPrescription();
        if (prescription == null) return false;

        return prescription.getSphLeft() != null
                || prescription.getSphRight() != null
                || prescription.getCylLeft() != null
                || prescription.getCylRight() != null
                || prescription.getAxisLeft() != null
                || prescription.getAxisRight() != null
                || prescription.getAddLeft() != null
                || prescription.getAddRight() != null;
    }

    @Override
    public long getTotalCustomersPaid() {
        return orderRepository.countCustomersPaid();
    }

    @Override
    public long getTotalOrdersPaid() {
        return orderRepository.count();
    }
}
