package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.*;
import com.fpt.glasseshop.entity.dto.ReturnRequestDTO;
import com.fpt.glasseshop.entity.dto.ReturnRequestResponseDTO;

import com.fpt.glasseshop.entity.dto.UpdateRefundBankInfoDTO;
import com.fpt.glasseshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.security.access.AccessDeniedException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReturnRequestService {

    private final ReturnRequestRepository returnRequestRepo;
    private final OrderRepository orderRepository;
    private final UserAccountRepository userAccountRepository;
    private final OrderItemRepository orderItemRepository;
    private final PrescriptionRepository prescriptionRepo;
    private final NotificationService notificationService;
    private final ProductVariantRepository proVariantRepo;


    private UserAccount getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString();
        System.out.println("EMAIL FROM SECURITY CONTEXT = " + email);
        System.out.println("TOKEN ROLE = " + role);
        if (email == null) {
            throw new AccessDeniedException("User is not authenticated");
        }

        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found"));
    }

    public ReturnRequestResponseDTO getById(Long requestId) {
        ReturnRequest request = returnRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Return request not found"));

        return mapToDTO(request);
    }
    private List<OrderItem> getComboItems(Order order) {
        if (order == null || order.getOrderItems() == null) {
            throw new RuntimeException("Order items not found");
        }

        boolean hasLens = order.getOrderItems().stream().anyMatch(i ->
                i.getLensOptionId() != null ||
                        i.getLensType() != null ||
                        "PRESCRIPTION".equalsIgnoreCase(i.getFulfillmentType())
        );

        boolean hasFrame = order.getOrderItems().stream().anyMatch(i ->
                i.getVariantId() != null
        );

        if (!(hasLens && hasFrame)) {
            throw new RuntimeException("This order is not a combo order");
        }

        return order.getOrderItems();
    }

    public ReturnRequestResponseDTO createReturnRequest(ReturnRequestDTO dto) throws BadRequestException {

        UserAccount currentUser = getCurrentUser();
        // combo
        if (Boolean.TRUE.equals(dto.getIsComboRequest())) {

            if (dto.getOrderId() == null) {
                throw new BadRequestException("Order id is required for combo return");
            }

            Order order = orderRepository.findById(dto.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // check owner
            if (order.getUser() == null || !order.getUser().getUserId().equals(currentUser.getUserId())) {
                throw new BadRequestException("You are not allowed to return this order");
            }

            // chỉ cho phép delivered
            if (!"DELIVERED".equalsIgnoreCase(order.getStatus())) {
                throw new BadRequestException("Only delivered orders can be returned");
            }

            // validate reason
            if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
                throw new BadRequestException("Reason is required");
            }

            ReturnRequest.RequestType requestType =
                    "EXCHANGE".equalsIgnoreCase(dto.getRequestType())
                            ? ReturnRequest.RequestType.EXCHANGE
                            : ReturnRequest.RequestType.RETURN;

            if (requestType == ReturnRequest.RequestType.RETURN) {
                validateRefundBankInfo(
                        dto.getBankAccountNumber(),
                        dto.getBankName(),
                        dto.getBankAccountHolder()
                );
            }

            // lấy item đầu tiên làm anchor
            OrderItem firstItem = order.getOrderItems().get(0);

            ReturnRequest request = ReturnRequest.builder()
                    .orderItem(firstItem)
                    .returnQuantity(dto.getReturnQuantity() != null ? dto.getReturnQuantity() : 1)
                    .reason(dto.getReason())
                    .description(dto.getDescription())
                    .imageUrl(dto.getImageUrl())
                    .status(ReturnRequest.ReturnStatus.PENDING)
                    .requestType(requestType)
                    .bankAccountNumber(requestType == ReturnRequest.RequestType.RETURN ? dto.getBankAccountNumber() : null)
                    .bankName(requestType == ReturnRequest.RequestType.RETURN ? dto.getBankName() : null)
                    .bankAccountHolder(requestType == ReturnRequest.RequestType.RETURN ? dto.getBankAccountHolder() : null)
                    .build();


            ReturnRequest saved = returnRequestRepo.save(request);

            return mapToDTO(saved);
        }

        OrderItem orderItem = orderItemRepository.findById(dto.getOrderItemId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        Order order = orderItem.getOrder();

        // 1. check owner
        if (order.getUser() == null || !order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new BadRequestException("You are not allowed to return this order item");
        }
        if (isComboOrderItem(orderItem)) {
            throw new BadRequestException(
                    "This product was purchased as a glasses + lens combo. Please return/exchange the whole combo instead of individual items."
            );
        }

        // 2. đơn delivered mới được đổi/trả
        if (!"DELIVERED".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException("Only delivered orders can be returned");
        }

        // 4. validate return quantity
        if (dto.getReturnQuantity() == null || dto.getReturnQuantity() <= 0) {
            throw new BadRequestException("Return quantity must be greater than 0");
        }

        if (dto.getReturnQuantity() > orderItem.getQuantity()) {
            throw new BadRequestException("Return quantity cannot exceed purchased quantity");
        }

        Integer requestedQty = returnRequestRepo.sumRequestedQuantityByOrderItemId(orderItem.getOrderItemId(),ReturnRequest.ReturnStatus.REJECTED);

        int remainingQty = orderItem.getQuantity() - (requestedQty != null ? requestedQty : 0);

        if (remainingQty <= 0) {
            throw new BadRequestException("All quantities of this item have already been requested for return/exchange");
        }

        if (dto.getReturnQuantity() > remainingQty) {
            throw new BadRequestException("Only " + remainingQty + " item(s) remaining for return/exchange");
        }

        // 5. check time 7 day
        if (order.getDeliveredAt() != null &&
                order.getDeliveredAt().plusDays(7).isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Return period expired");
        }

        // 6. validate reason
        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            throw new BadRequestException("Reason is required");
        }

//        if ("RETURN".equalsIgnoreCase(dto.getRequestType())) {
//            if (dto.getBankAccountNumber() == null || dto.getBankAccountNumber().trim().isEmpty()) {
//                throw new BadRequestException("Bank account number is required for refund");
//            }
//            if (dto.getBankName() == null || dto.getBankName().trim().isEmpty()) {
//                throw new BadRequestException("Bank name is required for refund");
//            }
//            if (dto.getBankAccountHolder() == null || dto.getBankAccountHolder().trim().isEmpty()) {
//                throw new BadRequestException("Bank account holder is required for refund");
//            }
//        }
        ReturnRequest.RequestType requestType =
                "EXCHANGE".equalsIgnoreCase(dto.getRequestType())
                        ? ReturnRequest.RequestType.EXCHANGE
                        : ReturnRequest.RequestType.RETURN;

        if (requestType == ReturnRequest.RequestType.RETURN) {
            validateRefundBankInfo(
                    dto.getBankAccountNumber(),
                    dto.getBankName(),
                    dto.getBankAccountHolder()
            );
        }

        ReturnRequest request = ReturnRequest.builder()
                .orderItem(orderItem)
                .returnQuantity(dto.getReturnQuantity())
                .reason(dto.getReason())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .status(ReturnRequest.ReturnStatus.PENDING)
                .requestType(requestType)
                .bankAccountNumber(requestType == ReturnRequest.RequestType.RETURN ? dto.getBankAccountNumber() : null)
                .bankName(requestType == ReturnRequest.RequestType.RETURN ? dto.getBankName() : null)
                .bankAccountHolder(requestType == ReturnRequest.RequestType.RETURN ? dto.getBankAccountHolder() : null)
                .build();

        ReturnRequest saved = returnRequestRepo.save(request);

        notificationService.notifyAdmins(
            "New Return Request",
            "A new " + saved.getRequestType() + " request has been submitted for item " + orderItem.getProductName(),
            "RETURN",
            saved.getRequestId()
        );

        return mapToDTO(saved);
    }

    private boolean isComboOrderItem(OrderItem targetItem) {
        Order order = targetItem.getOrder();
        if (order == null || order.getOrderItems() == null) return false;

        boolean hasLensInOrder = order.getOrderItems().stream().anyMatch(i ->
                i.getLensOptionId() != null ||
                        i.getLensType() != null ||
                        "PRESCRIPTION".equalsIgnoreCase(i.getFulfillmentType())
        );

        boolean hasFrameInOrder = order.getOrderItems().stream().anyMatch(i ->
                i.getVariantId() != null
        );

        return hasLensInOrder && hasFrameInOrder;
    }

    public List<ReturnRequestResponseDTO> getAll() {
        return returnRequestRepo.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    public List<ReturnRequestResponseDTO> getByOrderItemId(Long orderItemId) {
        return returnRequestRepo.findAllByOrderItemOrderItemId(orderItemId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public ReturnRequest approveRequest(Long requestId) {
        ReturnRequest request = returnRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Return request not found"));

        validatePendingRequest(request);

        if (request.getRequestType() == ReturnRequest.RequestType.EXCHANGE) {
            validateAndReserveStockForExchange(request);
            createReplacementOrderForExchange(request);
            request.setStatus(ReturnRequest.ReturnStatus.APPROVED);
        } else {
            request.setStatus(ReturnRequest.ReturnStatus.WAITING_CUSTOMER_RETURN);
        }

        request.setResolvedAt(LocalDateTime.now());

        ReturnRequest saved = returnRequestRepo.save(request);

        // notify SAU KHI set status
        UserAccount user = request.getOrderItem().getOrder().getUser();
        notificationService.createNotification(
                user,
                "Return Request Approved",
                request.getRequestType() == ReturnRequest.RequestType.RETURN
                        ? "Your return request has been approved. Please pack and send the item back."
                        : "Your exchange request has been approved.",
                "RETURN",
                request.getRequestId()
        );

        return saved;
    }
    //nhận hàng
    @Transactional
    public ReturnRequest markAsReceivedReturn(Long requestId) {
        ReturnRequest request = returnRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Return request not found"));

        if (request.getRequestType() != ReturnRequest.RequestType.RETURN) {
            throw new RuntimeException("Only return requests can be marked as received");
        }

        if (request.getStatus() != ReturnRequest.ReturnStatus.WAITING_CUSTOMER_RETURN) {
            throw new RuntimeException("Only waiting return requests can be marked as received");
        }

        request.setStatus(ReturnRequest.ReturnStatus.RECEIVED_RETURN);

        ReturnRequest saved = returnRequestRepo.save(request);

        UserAccount user = request.getOrderItem().getOrder().getUser();
        notificationService.createNotification(
                user,
                "Returned Item Received",
                "We have received your returned item and will process the refund soon.",
                "RETURN",
                request.getRequestId()
        );

        return saved;
    }
    @Transactional
    public ReturnRequest markRefundPending(Long requestId) {
        ReturnRequest request = returnRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Return request not found"));

        if (request.getRequestType() != ReturnRequest.RequestType.RETURN) {
            throw new RuntimeException("Only return requests can move to refund pending");
        }

        if (request.getStatus() != ReturnRequest.ReturnStatus.RECEIVED_RETURN
                && request.getStatus() != ReturnRequest.ReturnStatus.REFUND_INFO_INVALID) {
            throw new RuntimeException("Only received or corrected requests can move to refund pending");
        }

        request.setStatus(ReturnRequest.ReturnStatus.REFUND_PENDING);
        request.setRefundNote(null);

        ReturnRequest saved = returnRequestRepo.save(request);

        UserAccount user = request.getOrderItem().getOrder().getUser();
        notificationService.createNotification(
                user,
                "Refund Processing",
                "Your refund is being processed.",
                "RETURN",
                request.getRequestId()
        );

        return saved;
    }
    @Transactional
    public ReturnRequest markRefundInfoInvalid(Long requestId, String note) {
        ReturnRequest request = returnRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Return request not found"));

        if (request.getRequestType() != ReturnRequest.RequestType.RETURN) {
            throw new RuntimeException("Only return requests can have invalid refund info");
        }

        if (request.getStatus() != ReturnRequest.ReturnStatus.RECEIVED_RETURN
                && request.getStatus() != ReturnRequest.ReturnStatus.REFUND_PENDING) {
            throw new RuntimeException("Refund info can only be marked invalid after receiving returned item");
        }

        if (note == null || note.trim().isEmpty()) {
            throw new RuntimeException("Refund note is required");
        }

        request.setStatus(ReturnRequest.ReturnStatus.REFUND_INFO_INVALID);
        request.setRefundNote(note.trim());

        ReturnRequest saved = returnRequestRepo.save(request);

        UserAccount user = request.getOrderItem().getOrder().getUser();
        notificationService.createNotification(
                user,
                "Refund Information Invalid",
                "Your refund information is invalid. Reason: " + note,
                "RETURN",
                request.getRequestId()
        );

        return saved;
    }
    @Transactional
    public ReturnRequestResponseDTO updateRefundBankInfo(Long requestId, UpdateRefundBankInfoDTO dto) {
        ReturnRequest request = returnRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Return request not found"));

        UserAccount currentUser = getCurrentUser();

        OrderItem orderItem = request.getOrderItem();
        if (orderItem == null || orderItem.getOrder() == null ||
                orderItem.getOrder().getUser() == null ||
                !orderItem.getOrder().getUser().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("You are not allowed to update this refund info");
        }

        if (request.getRequestType() != ReturnRequest.RequestType.RETURN) {
            throw new RuntimeException("Only return requests can update refund bank info");
        }

        if (request.getStatus() != ReturnRequest.ReturnStatus.REFUND_INFO_INVALID) {
            throw new RuntimeException("Refund info can only be updated when marked invalid");
        }

        validateRefundBankInfo(
                dto.getBankAccountNumber(),
                dto.getBankName(),
                dto.getBankAccountHolder()
        );

        request.setBankAccountNumber(dto.getBankAccountNumber());
        request.setBankName(dto.getBankName());
        request.setBankAccountHolder(dto.getBankAccountHolder());
        request.setRefundNote(null);
        request.setStatus(ReturnRequest.ReturnStatus.REFUND_PENDING);

        ReturnRequest saved = returnRequestRepo.save(request);

        notificationService.notifyAdmins(
                "Refund Info Updated",
                "Customer has updated refund information for request #" + requestId,
                "RETURN",
                requestId
        );

        return mapToDTO(saved);
    }
    @Transactional
    public ReturnRequest markAsRefunded(Long requestId) {
        ReturnRequest request = returnRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Return request not found"));

        if (request.getRequestType() != ReturnRequest.RequestType.RETURN) {
            throw new RuntimeException("Only return requests can be refunded");
        }

        if (request.getStatus() != ReturnRequest.ReturnStatus.RECEIVED_RETURN
                && request.getStatus() != ReturnRequest.ReturnStatus.REFUND_PENDING) {
            throw new RuntimeException("Only eligible requests can be refunded");
        }

        request.setStatus(ReturnRequest.ReturnStatus.REFUNDED);
        request.setResolvedAt(LocalDateTime.now());

        ReturnRequest saved = returnRequestRepo.save(request);

        UserAccount user = request.getOrderItem().getOrder().getUser();
        notificationService.createNotification(
                user,
                "Refund Completed",
                "Your refund has been successfully processed.",
                "RETURN",
                request.getRequestId()
        );

        return saved;
    }
    private void validateRefundBankInfo(String bankAccountNumber, String bankName, String bankAccountHolder) {
        if (bankAccountNumber == null || bankAccountNumber.trim().isEmpty()) {
            throw new RuntimeException("Bank account number is required for refund");
        }
        if (bankName == null || bankName.trim().isEmpty()) {
            throw new RuntimeException("Bank name is required for refund");
        }
        if (bankAccountHolder == null || bankAccountHolder.trim().isEmpty()) {
            throw new RuntimeException("Bank account holder is required for refund");
        }
    }
    private void validateAndReserveStockForExchange(ReturnRequest request) {
        OrderItem orderItem = getRequiredOrderItem(request);

        if (orderItem.getVariantId() == null) {
            throw new RuntimeException("Variant not found for exchange item");
        }

        ProductVariant variant = proVariantRepo.findById(orderItem.getVariantId())
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        Integer requestedQty = request.getReturnQuantity();
        Integer currentStock = variant.getStockQuantity();

        if (currentStock == null || currentStock <= 0) {
            throw new RuntimeException("This product is out of stock, cannot process exchange");
        }

        if (currentStock < requestedQty) {
            throw new RuntimeException(
                    "Not enough stock for exchange. Available: " + currentStock
                            + ", requested: " + requestedQty
            );
        }

        variant.setStockQuantity(currentStock - requestedQty);
        proVariantRepo.save(variant);
    }

    @Transactional
    public ReturnRequest rejectRequest(Long requestId, String rejectionReason) {
        ReturnRequest request = returnRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Return request not found"));

        validatePendingRequest(request);

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new RuntimeException("Rejection reason is required");
        }

        request.setStatus(ReturnRequest.ReturnStatus.REJECTED);
        request.setRejectionReason(rejectionReason.trim());
        request.setResolvedAt(LocalDateTime.now());

        ReturnRequest saved = returnRequestRepo.save(request);

        // Notify user
        UserAccount user = request.getOrderItem().getOrder().getUser();
        notificationService.createNotification(
            user,
            "Return Request Rejected",
            "Your " + request.getRequestType() + " request for " + request.getOrderItem().getProductName() + " has been rejected. Reason: " + rejectionReason,
            "RETURN",
            request.getRequestId()
        );

        return saved;
    }

    @Transactional
    public ReturnRequest completeRequest(Long requestId) {
        ReturnRequest request = returnRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Return request not found"));

        if (request.getStatus() != ReturnRequest.ReturnStatus.APPROVED) {
            throw new RuntimeException("Only approved requests can be completed");
        }

        request.setStatus(ReturnRequest.ReturnStatus.COMPLETED);
        request.setResolvedAt(LocalDateTime.now());

        ReturnRequest saved = returnRequestRepo.save(request);

        // Notify user
        UserAccount user = request.getOrderItem().getOrder().getUser();
        notificationService.createNotification(
            user,
            "Return Process Completed",
            "The " + request.getRequestType() + " process for " + request.getOrderItem().getProductName() + " is now complete.",
            "RETURN",
            request.getRequestId()
        );

        return saved;
    }

    public ReturnRequestResponseDTO mapToDTO(ReturnRequest request) {
        OrderItem orderItem = request.getOrderItem();

        return ReturnRequestResponseDTO.builder()
                .requestId(request.getRequestId())
                .orderId(orderItem != null && orderItem.getOrder() != null ? orderItem.getOrder().getOrderId() : null)
                .orderItemId(orderItem != null ? orderItem.getOrderItemId() : null)
                .returnQuantity(request.getReturnQuantity())
                .reason(request.getReason())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .status(request.getStatus() != null ? request.getStatus().name() : null)
                .rejectionReason(request.getRejectionReason())
                .requestType(request.getRequestType() != null ? request.getRequestType().name() : null)
                .replacementOrderId(request.getReplacementOrderId())
                .replacementOrderItemId(request.getReplacementOrderItemId())
                .requestedAt(request.getRequestedAt())
                .resolvedAt(request.getResolvedAt())
                .productName(orderItem != null ? orderItem.getProductName() : null)
                .productImageUrl(orderItem != null ? orderItem.getImageUrl() : null)
                .variantColor(orderItem != null ? orderItem.getVariantColor() : null)
                .variantSize(orderItem != null ? orderItem.getVariantSize() : null)
                .purchasedQuantity(orderItem != null ? orderItem.getQuantity() : null)
                .unitPrice(orderItem != null ? orderItem.getUnitPrice() : null)
                .lensType(orderItem != null ? orderItem.getLensType() : null)
                .lensCoating(orderItem != null ? orderItem.getLensCoating() : null)
                .bankAccountNumber(request.getBankAccountNumber())
                .bankName(request.getBankName())
                .bankAccountHolder(request.getBankAccountHolder())
                .refundNote(request.getRefundNote())
                .build();
    }


    private void validatePendingRequest(ReturnRequest request) {
        if (request.getStatus() != ReturnRequest.ReturnStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be updated");
        }
    }

    private void createReplacementOrderForExchange(ReturnRequest request) {
        OrderItem anchorItem = getRequiredOrderItem(request);
        Order oldOrder = anchorItem.getOrder();
        Integer exchangeQty = request.getReturnQuantity();

        List<OrderItem> sourceItems;

        // nếu order gốc là combo thì clone cả bộ
        if (isComboOrderItem(anchorItem)) {
            sourceItems = getComboItems(oldOrder);
        } else {
            sourceItems = List.of(anchorItem);
        }

        Order newOrder = createReplacementOrderFromItems(sourceItems, exchangeQty);

        OrderItem firstSavedItem = null;

        for (OrderItem oldItem : sourceItems) {
            OrderItem cloned = cloneOrderItem(oldItem, newOrder, exchangeQty);
            OrderItem savedNewItem = orderItemRepository.save(cloned);

            clonePrescriptionIfNeeded(oldItem, savedNewItem);

            if (firstSavedItem == null) {
                firstSavedItem = savedNewItem;
            }
        }

        request.setReplacementOrderId(newOrder.getOrderId());
        request.setReplacementOrderItemId(firstSavedItem != null ? firstSavedItem.getOrderItemId() : null);
    }
    private Order createReplacementOrderFromItems(List<OrderItem> items, Integer quantity) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("No items found for replacement order");
        }

        Order oldOrder = items.get(0).getOrder();

        BigDecimal totalPrice = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(quantity)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return orderRepository.save(
                Order.builder()
                        .user(oldOrder.getUser())
                        .status("PENDING")
                        .paymentStatus("PAID")
                        .paymentMethod("EXCHANGE")
                        .shippingAddress(oldOrder.getShippingAddress())
                        .billingAddress(oldOrder.getBillingAddress())
                        .totalPrice(totalPrice)
                        .build()
        );
    }

    private OrderItem getRequiredOrderItem(ReturnRequest request) {
        OrderItem orderItem = request.getOrderItem();
        if (orderItem == null) {
            throw new RuntimeException("Order item not found");
        }
        if (orderItem.getOrder() == null) {
            throw new RuntimeException("Original order not found");
        }
        return orderItem;
    }

    private Order createReplacementOrder(OrderItem oldItem, Integer quantity) {
        Order oldOrder = oldItem.getOrder();

        BigDecimal totalPrice = oldItem.getUnitPrice()
                .multiply(BigDecimal.valueOf(quantity));

        return orderRepository.save(
                Order.builder()
                        .user(oldOrder.getUser())
                        .status("PENDING")
                        .paymentStatus("PAID")
                        .paymentMethod("EXCHANGE")
                        .shippingAddress(oldOrder.getShippingAddress())
                        .billingAddress(oldOrder.getBillingAddress())
                        .totalPrice(totalPrice)
                        .build()
        );
    }

    private OrderItem cloneOrderItem(OrderItem oldItem, Order newOrder, Integer quantity) {
        return OrderItem.builder()
                .order(newOrder)
                .variant(oldItem.getVariant())
                .lensOption(oldItem.getLensOption())
                .quantity(quantity)
                .unitPrice(oldItem.getUnitPrice())
                .fulfillmentType(oldItem.getFulfillmentType())

                .variantId(oldItem.getVariantId())
                .productId(oldItem.getProductId())
                .productName(oldItem.getProductName())
                .variantColor(oldItem.getVariantColor())
                .variantSize(oldItem.getVariantSize())
                .imageUrl(oldItem.getImageUrl())

                .lensType(oldItem.getLensType())
                .lensPrice(oldItem.getLensPrice())
                .lensCoating(oldItem.getLensCoating())
                .lensOptionId(oldItem.getLensOptionId())

                .sphLeft(oldItem.getSphLeft())
                .sphRight(oldItem.getSphRight())
                .cylLeft(oldItem.getCylLeft())
                .cylRight(oldItem.getCylRight())
                .axisLeft(oldItem.getAxisLeft())
                .axisRight(oldItem.getAxisRight())
                .addLeft(oldItem.getAddLeft())
                .addRight(oldItem.getAddRight())


                .isPreorder(oldItem.getIsPreorder())
                .build();
    }

    private void clonePrescriptionIfNeeded(OrderItem oldItem, OrderItem newItem) {
        Prescription oldPrescription = oldItem.getPrescription();
        if (oldPrescription == null) return;

        prescriptionRepo.save(
                Prescription.builder()
                        .orderItem(newItem)
                        .user(oldPrescription.getUser())
                        .name(oldPrescription.getName())
                        .sphLeft(oldPrescription.getSphLeft())
                        .sphRight(oldPrescription.getSphRight())
                        .cylLeft(oldPrescription.getCylLeft())
                        .cylRight(oldPrescription.getCylRight())
                        .axisLeft(oldPrescription.getAxisLeft())
                        .axisRight(oldPrescription.getAxisRight())
                        .addLeft(oldPrescription.getAddLeft())
                        .addRight(oldPrescription.getAddRight())

                        .doctorName(oldPrescription.getDoctorName())
                        .expirationDate(oldPrescription.getExpirationDate())
                        .status(oldPrescription.getStatus())
                        .adminNote(oldPrescription.getAdminNote())
                        .build()
        );
    }
}
