package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.*;
import com.fpt.glasseshop.entity.dto.*;
import com.fpt.glasseshop.exception.ResourceNotFoundException;
import com.fpt.glasseshop.repository.CartRepository;
import com.fpt.glasseshop.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemService orderItemService;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final com.fpt.glasseshop.repository.ProductVariantRepository productVariantRepository;
    private final NotificationService notificationService;

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

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserUserId(userId);
    }

    public List<OrderDTO> getOrdersDTOByUserId(Long userId) {
        return getOrdersByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<OrderDTO> getAllOrdersDTO() {
        return orderRepository.findAllByOrderByOrderDateDesc().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<Order> getOrderById(Long orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            // Check for 12h timeout for preorder balance payment
            if ("PARTIAL".equals(order.getDepositType()) && 
                "PROCESSING".equals(order.getStatus()) && 
                order.getStockReadyAt() != null && 
                order.getStockReadyAt().plusHours(12).isBefore(LocalDateTime.now()) &&
                "UNPAID".equals(order.getPaymentStatus()) &&
                !"COD".equals(order.getPaymentMethod())) {
                
                order.setPaymentMethod("COD");
                orderRepository.save(order);
            }
        }
        return orderOpt;
    }

    public Optional<OrderDTO> getOrderDTOById(Long orderId) {
        return getOrderById(orderId).map(this::convertToDTO);
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        java.util.List<String> validStatuses = java.util.Arrays.asList(
            "PENDING", "PROCESSING", "DELIVERING", "DELIVERED", "CANCELED", 
            "CANCELLED", "SHIPPED", "PREORDER", "COMPLETED"
        );
        String targetStatus = newStatus.toUpperCase();
        if (!validStatuses.contains(targetStatus)) {
            throw new IllegalArgumentException("Invalid order status: " + targetStatus);
        }

        // Force StockReadyAt if moving from PENDING/PREORDER to PROCESSING (approving pre-order)
        if (("PENDING".equals(order.getStatus()) || "PREORDER".equals(order.getStatus())) && 
            "PROCESSING".equals(targetStatus)) {
            order.setStockReadyAt(LocalDateTime.now());
        }

        // Logic for Pre-order: cannot move to SHIPPED/DELIVERING if balance not paid (unless COD)
        if ("PROCESSING".equals(order.getStatus()) && ("SHIPPED".equals(targetStatus) || "DELIVERING".equals(targetStatus))) {
            if ("PARTIAL".equals(order.getDepositType()) && 
                "UNPAID".equals(order.getPaymentStatus()) && 
                !"COD".equals(order.getPaymentMethod())) {
                throw new IllegalStateException("Cannot ship pre-order: balance payment is required or must be COD.");
            }

            // Check for unapproved prescriptions
            boolean hasUnapprovedPrescription = order.getOrderItems().stream()
                .anyMatch(item -> item.getPrescription() != null && !Boolean.TRUE.equals(item.getPrescription().getStatus()));
            if (hasUnapprovedPrescription) {
                throw new IllegalStateException("Cannot ship order: all prescriptions must be approved first.");
            }
        }

        order.setStatus(targetStatus);

        // Tự động chuyển paymentStatus sang PAID khi đơn hàng hoàn thành hoặc đã giao
        if ("DELIVERED".equalsIgnoreCase(targetStatus) || "COMPLETED".equalsIgnoreCase(targetStatus)) {
            order.setPaymentStatus("PAID");
            if (order.getDeliveredAt() == null) {
                order.setDeliveredAt(LocalDateTime.now());
            }
        }

        notificationService.createNotification(
            order.getUser(), 
            "Order Status Updated", 
            "Your order " + order.getOrderCode() + " is now " + targetStatus,
            "ORDER",
            order.getOrderId()
        );

        // Special notification for pre-order ready
        if ("PROCESSING".equals(targetStatus) && order.getOrderItems().stream().anyMatch(i -> Boolean.TRUE.equals(i.getIsPreorder()))) {
             notificationService.createNotification(
                order.getUser(), 
                "Pre-order Item Ready", 
                "Your pre-order items in " + order.getOrderCode() + " are now in stock and being processed.",
                "ORDER",
                order.getOrderId()
            );
        }

        // Check if canceled to restore stock
        if ("CANCELED".equals(targetStatus) || "CANCELLED".equals(targetStatus)) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getVariantId() != null && item.getQuantity() != null) {
                    productVariantRepository.decreaseStock(item.getVariantId(), -item.getQuantity()); // negative decrease = increase
                }
            }
        }
        return convertToDTO(orderRepository.save(order));
    }

    @Transactional
    public OrderDTO updatePaymentOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        java.util.List<String> validStatuses = java.util.Arrays.asList("UNPAID", "PAID", "PAID_FULL");
        if (!validStatuses.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid payment status: " + newStatus);
        }

        // Logic for Pre-order Balance: if already PAID (deposit), and receiving PAID again -> set to PAID_FULL
        if ("PAID".equals(order.getPaymentStatus()) && "PAID".equals(newStatus)) {
            order.setPaymentStatus("PAID_FULL");
        } else {
            order.setPaymentStatus(newStatus);
        }

        // Check if canceled to restore stock
        if ("CANCELED".equals(newStatus)) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getVariantId() != null && item.getQuantity() != null) {
                    productVariantRepository.decreaseStock(item.getVariantId(), -item.getQuantity()); // negative decrease = increase
                }
            }
        }

        return convertToDTO(orderRepository.save(order));
    }

    @Transactional
    public OrderDTO updatePaymentMethod(Long orderId, String newMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        order.setPaymentMethod(newMethod);
        return convertToDTO(orderRepository.save(order));
    }

    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Order not found with id: " + id);
        }
        orderRepository.deleteById(id);
    }

    @Transactional
    public OrderDTO createOrderFromCart(UserAccount user, CreateOrderRequest request) {
        // 0. Idempotency Check
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().trim().isEmpty()) {
            Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existingOrder.isPresent()) {
                return convertToDTO(existingOrder.get());
            }
        }

        // 1. Get User's Cart with Pessimistic Lock for Concurrency
        Cart cart = cartRepository.findByUserUserIdForCheckout(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + user.getUserId()));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot create order from an empty cart");
        }

        // 2. Initialize Calculations
        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal shippingFee = request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.ZERO;
        BigDecimal voucherDiscount = request.getVoucherDiscount() != null ? request.getVoucherDiscount() : BigDecimal.ZERO;

        // 3. Create Order Object
        String orderCode = "ORD-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();

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
                .status("PENDING")
                .paymentStatus("UNPAID")
                .depositType(request.getDepositType())
                .depositPaymentMethod(request.getPaymentMethod())
                .status(request.getShipmentStatus() != null ? request.getShipmentStatus() : "PENDING")
                .orderDate(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .build();

        // 4. Create OrderItems from CartItems and Calculate Total
        for (CartItem cartItem : cart.getItems()) {
            if (cartItem.getQuantity() == null || cartItem.getQuantity() <= 0) {
                throw new IllegalArgumentException("Invalid quantity for cart item");
            }

            // Atomic Stock Validation & Deduction (Skip for Preorders)
            boolean isPreorderItem = Boolean.TRUE.equals(cartItem.getIsPreorder());

            if (!isPreorderItem) {
                int updatedRows = productVariantRepository.decreaseStock(cartItem.getVariant().getVariantId(), cartItem.getQuantity());
                if (updatedRows == 0) {
                    throw new IllegalArgumentException("Insufficient stock for product: " + (cartItem.getVariant().getProduct() != null ? cartItem.getVariant().getProduct().getName() : "Unknown"));
                }
            }

            BigDecimal unitPrice = cartItem.getPrice() != null ? cartItem.getPrice() : BigDecimal.ZERO;
            BigDecimal lensPrice = (cartItem.getLensOption() != null && cartItem.getLensOption().getPrice() != null)
                    ? cartItem.getLensOption().getPrice()
                    : BigDecimal.ZERO;

            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalPrice = totalPrice.add(subtotal);

            // Merged Logic: Loop from 'dev' + Prescription fields from 'NVK'
            for (int i = 0; i < cartItem.getQuantity(); i++) {

                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .variant(cartItem.getVariant())
                        .variantId(cartItem.getVariant() != null ? cartItem.getVariant().getVariantId() : null)
                        .productId(cartItem.getVariant() != null && cartItem.getVariant().getProduct() != null
                                ? cartItem.getVariant().getProduct().getProductId()
                                : (cartItem.getProductId() != null ? cartItem.getProductId() : null))
                        .productName(cartItem.getVariant() != null && cartItem.getVariant().getProduct() != null
                                ? cartItem.getVariant().getProduct().getName()
                                : (cartItem.getProductName() != null ? cartItem.getProductName() : null))
                        .variantColor(cartItem.getVariant() != null ? cartItem.getVariant().getColor() : null)
                        .variantSize(cartItem.getVariant() != null ? cartItem.getVariant().getFrameSize() : null)
                        .imageUrl(cartItem.getVariant() != null ? cartItem.getVariant().getImageUrl() : null)
                        .lensOption(cartItem.getLensOption())
                        .lensOptionId(cartItem.getLensOption() != null ? cartItem.getLensOption().getLensOptionId() : null)
                        .lensType(cartItem.getLensOption() != null ? cartItem.getLensOption().getType() : null)
                        .lensPrice(lensPrice)
                        .lensCoating(cartItem.getLensOption() != null ? cartItem.getLensOption().getCoating() : null)
                        .quantity(1) // QUAN TRỌNG
                        .unitPrice(unitPrice)
                        .isPreorder(isPreorderItem)
                        .fulfillmentType(cartItem.getPrescription() != null || cartItem.getIsLens() == Boolean.TRUE
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

        // ✅ CALC DEPOSIT
        if (Boolean.TRUE.equals(request.getIsPreorder()) && "PARTIAL".equals(request.getDepositType())) {
            BigDecimal inStockPart = BigDecimal.ZERO;
            BigDecimal preOrderPart = BigDecimal.ZERO;
            
            for (OrderItem item : order.getOrderItems()) {
                BigDecimal itemSubtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                if (Boolean.TRUE.equals(item.getIsPreorder())) {
                    preOrderPart = preOrderPart.add(itemSubtotal);
                } else {
                    inStockPart = inStockPart.add(itemSubtotal);
                }
            }
            // Partial: In-stock(100%) + Preorder(50%) + Shipping(100%)
            order.setDepositAmount(inStockPart.add(preOrderPart.divide(BigDecimal.valueOf(2))).add(shippingFee));
        } else {
            order.setDepositAmount(finalTotal);
        }

        // 5. Save Order
        Order savedOrder = orderRepository.save(order);

        notificationService.notifyAdmins(
            "New Order Received", 
            "A new order " + savedOrder.getOrderCode() + " has been placed by " + savedOrder.getFullName(),
            "ORDER",
            savedOrder.getOrderId()
        );

        // 6. Clear Cart (DEFERRED: only clear on SUCCESSFUL payment/COD)
        // cartService.clearCart(user);

        return convertToDTO(savedOrder);
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemService.getOrderItemsByOrderId(orderId);
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
                .stockReadyAt(order.getStockReadyAt())
                .orderItems(order.getOrderItems() != null ? order.getOrderItems().stream()
                        .map(this::mapToItemDTO)
                        .collect(Collectors.toList()) : null)
                .totalItems(order.getOrderItems() != null ? order.getOrderItems().size() : 0)
                .build();
    }

    private OrderItemDTO mapToItemDTO(OrderItem item) {
        if (item == null)
            return null;
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
                .subtotal(item.getUnitPrice().multiply(new java.math.BigDecimal(item.getQuantity())))
                .fulfillmentType(item.getFulfillmentType())
                .itemType(item.getItemType())
                .isPreorder(item.getIsPreorder())
                // Populating manual prescription entry data in DTO
                .sphLeft(item.getSphLeft())
                .sphRight(item.getSphRight())
                .cylLeft(item.getCylLeft())
                .cylRight(item.getCylRight())
                .axisLeft(item.getAxisLeft())
                .axisRight(item.getAxisRight())
                .addLeft(item.getAddLeft())
                .addRight(item.getAddRight())

                .prescription(mapToPrescriptionDTO(item.getPrescription()))
                .build();
    }

    private PrescriptionDTO mapToPrescriptionDTO(Prescription p) {
        if (p == null)
            return null;
        return PrescriptionDTO.builder()
                .prescriptionId(p.getPrescriptionId())
                .orderItemId(p.getOrderItem().getOrderItemId())
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

    public long getTotalCustomersPaid() {
        return orderRepository.countCustomersPaid();
    }

    public long getTotalOrdersPaid() {
        return orderRepository.count();
    }

}