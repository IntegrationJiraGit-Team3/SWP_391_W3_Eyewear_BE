package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.*;
import com.fpt.glasseshop.entity.dto.*;
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
    private final ReturnRequestTransactionRepository returnRequestTransactionRepository;
    private final OrderRepository orderRepository;
    private final UserAccountRepository userAccountRepository;
    private final OrderItemRepository orderItemRepository;
    private final PrescriptionRepository prescriptionRepo;
    private final NotificationService notificationService;
    private final ProductVariantRepository proVariantRepo;

    private UserAccount getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
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

        boolean hasFrame = order.getOrderItems().stream().anyMatch(i -> i.getVariantId() != null);

        if (!(hasLens && hasFrame)) {
            throw new RuntimeException("This order is not a combo order");
        }

        return order.getOrderItems();
    }

    @Transactional
    public ReturnRequestResponseDTO createReturnRequest(ReturnRequestDTO dto) throws BadRequestException {
        UserAccount currentUser = getCurrentUser();

        if (Boolean.TRUE.equals(dto.getIsComboRequest())) {
            if (dto.getOrderId() == null) {
                throw new BadRequestException("Order id is required for combo return");
            }

            Order order = orderRepository.findById(dto.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            if (order.getUser() == null || !order.getUser().getUserId().equals(currentUser.getUserId())) {
                throw new BadRequestException("You are not allowed to return this order");
            }

            if (!"DELIVERED".equalsIgnoreCase(order.getStatus()) && !"COMPLETED".equalsIgnoreCase(order.getStatus())) {
                throw new BadRequestException("Only delivered orders can be returned");
            }

            if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
                throw new BadRequestException("Reason is required");
            }

            ReturnRequest.RequestType requestType =
                    "EXCHANGE".equalsIgnoreCase(dto.getRequestType())
                            ? ReturnRequest.RequestType.EXCHANGE
                            : ReturnRequest.RequestType.RETURN;

            if (requestType == ReturnRequest.RequestType.RETURN) {
                validateRefundBankInfo(dto.getBankAccountNumber(), dto.getBankName(), dto.getBankAccountHolder());
            }

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
            logTransaction(saved, ReturnRequestTransaction.Action.CREATED, null, ReturnRequest.ReturnStatus.PENDING, null, null, null, null);
            return mapToDTO(saved);
        }

        OrderItem orderItem = orderItemRepository.findById(dto.getOrderItemId())
                .orElseThrow(() -> new RuntimeException("Order item not found"));
        Order order = orderItem.getOrder();

        if (order.getUser() == null || !order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new BadRequestException("You are not allowed to return this order item");
        }

        if (isComboOrderItem(orderItem)) {
            throw new BadRequestException(
                    "This product was purchased as a glasses + lens combo. Please return/exchange the whole combo instead of individual items."
            );
        }

        if (!"DELIVERED".equalsIgnoreCase(order.getStatus()) && !"COMPLETED".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException("Only delivered orders can be returned");
        }

        if (dto.getReturnQuantity() == null || dto.getReturnQuantity() <= 0) {
            throw new BadRequestException("Return quantity must be greater than 0");
        }

        if (dto.getReturnQuantity() > orderItem.getQuantity()) {
            throw new BadRequestException("Return quantity cannot exceed purchased quantity");
        }

        Integer requestedQty = returnRequestRepo.sumRequestedQuantityByOrderItemId(
                orderItem.getOrderItemId(),
                ReturnRequest.ReturnStatus.REJECTED
        );

        int remainingQty = orderItem.getQuantity() - (requestedQty != null ? requestedQty : 0);

        if (remainingQty <= 0) {
            throw new BadRequestException("All quantities of this item have already been requested for return/exchange");
        }

        if (dto.getReturnQuantity() > remainingQty) {
            throw new BadRequestException("Only " + remainingQty + " item(s) remaining for return/exchange");
        }

        if (order.getDeliveredAt() != null && order.getDeliveredAt().plusDays(7).isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Return period expired");
        }

        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            throw new BadRequestException("Reason is required");
        }

        ReturnRequest.RequestType requestType =
                "EXCHANGE".equalsIgnoreCase(dto.getRequestType())
                        ? ReturnRequest.RequestType.EXCHANGE
                        : ReturnRequest.RequestType.RETURN;

        if (requestType == ReturnRequest.RequestType.RETURN) {
            validateRefundBankInfo(dto.getBankAccountNumber(), dto.getBankName(), dto.getBankAccountHolder());
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
        logTransaction(saved, ReturnRequestTransaction.Action.CREATED, null, ReturnRequest.ReturnStatus.PENDING, null, null, null, null);

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

        boolean hasFrameInOrder = order.getOrderItems().stream().anyMatch(i -> i.getVariantId() != null);

        return hasLensInOrder && hasFrameInOrder;
    }

    public List<ReturnRequestResponseDTO> getAll() {
        return returnRequestRepo.findAll().stream().map(this::mapToDTO).toList();
    }

    public List<ReturnRequestResponseDTO> getByOrderItemId(Long orderItemId) {
        return returnRequestRepo.findAllByOrderItemOrderItemId(orderItemId).stream().map(this::mapToDTO).toList();
    }

    @Transactional
    public ReturnRequest approveRequest(Long requestId) {
        ReturnRequest request = returnRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Return request not found"));

        validatePendingRequest(request);
        ReturnRequest.ReturnStatus before = request.getStatus();

        if (request.getRequestType() == ReturnRequest.RequestType.EXCHANGE) {
            validateAndReserveStockForExchange(request);
            createReplacementOrderForExchange(request);
            request.setStatus(ReturnRequest.ReturnStatus.APPROVED);
        } else {
            request.setStatus(ReturnRequest.ReturnStatus.WAITING_CUSTOMER_RETURN);
        }

        request.setResolvedAt(LocalDateTime.now());
        ReturnRequest saved = returnRequestRepo.save(request);

        logTransaction(saved, ReturnRequestTransaction.Action.APPROVED, before, saved.getStatus(), null, null, null, null);

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

        ReturnRequest.ReturnStatus before = request.getStatus();
        request.setStatus(ReturnRequest.ReturnStatus.RECEIVED_RETURN);
        ReturnRequest saved = returnRequestRepo.save(request);

        logTransaction(saved, ReturnRequestTransaction.Action.RECEIVED_RETURN, before, saved.getStatus(), null, null, null, null);

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

        ReturnRequest.ReturnStatus before = request.getStatus();
        request.setStatus(ReturnRequest.ReturnStatus.REFUND_PENDING);
        request.setRefundNote(null);
        ReturnRequest saved = returnRequestRepo.save(request);

        logTransaction(saved, ReturnRequestTransaction.Action.REFUND_PENDING, before, saved.getStatus(), null, null, null, null);

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

        ReturnRequest.ReturnStatus before = request.getStatus();
        request.setStatus(ReturnRequest.ReturnStatus.REFUND_INFO_INVALID);
        request.setRefundNote(note.trim());
        ReturnRequest saved = returnRequestRepo.save(request);

        logTransaction(saved, ReturnRequestTransaction.Action.REFUND_INFO_INVALID, before, saved.getStatus(), null, null, null, note.trim());

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

        if (orderItem == null || orderItem.getOrder() == null || orderItem.getOrder().getUser() == null
                || !orderItem.getOrder().getUser().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("You are not allowed to update this refund info");
        }

        if (request.getRequestType() != ReturnRequest.RequestType.RETURN) {
            throw new RuntimeException("Only return requests can update refund bank info");
        }

        if (request.getStatus() != ReturnRequest.ReturnStatus.REFUND_INFO_INVALID) {
            throw new RuntimeException("Refund info can only be updated when marked invalid");
        }

        validateRefundBankInfo(dto.getBankAccountNumber(), dto.getBankName(), dto.getBankAccountHolder());

        ReturnRequest.ReturnStatus before = request.getStatus();
        request.setBankAccountNumber(dto.getBankAccountNumber());
        request.setBankName(dto.getBankName());
        request.setBankAccountHolder(dto.getBankAccountHolder());
        request.setRefundNote(null);
        request.setStatus(ReturnRequest.ReturnStatus.REFUND_PENDING);
        ReturnRequest saved = returnRequestRepo.save(request);

        logTransaction(saved, ReturnRequestTransaction.Action.REFUND_INFO_UPDATED, before, saved.getStatus(), null, null, null, null);

        notificationService.notifyAdmins(
                "Refund Info Updated",
                "Customer has updated refund information for request #" + requestId,
                "RETURN",
                requestId
        );

        return mapToDTO(saved);
    }

    @Transactional
    public ReturnRequest markAsRefunded(Long requestId, RefundProcessDTO dto) {
        ReturnRequest request = returnRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Return request not found"));

        if (request.getRequestType() != ReturnRequest.RequestType.RETURN) {
            throw new RuntimeException("Only return requests can be refunded");
        }

        if (request.getStatus() != ReturnRequest.ReturnStatus.RECEIVED_RETURN
                && request.getStatus() != ReturnRequest.ReturnStatus.REFUND_PENDING) {
            throw new RuntimeException("Only eligible requests can be refunded");
        }

        if (dto == null) {
            throw new RuntimeException("Refund information is required");
        }

        if (dto.getPaymentMethod() == null || !"BANK_TRANSFER".equalsIgnoreCase(dto.getPaymentMethod().trim())) {
            throw new RuntimeException("Only BANK_TRANSFER is supported");
        }

        if (dto.getTransactionReference() == null || dto.getTransactionReference().trim().isEmpty()) {
            throw new RuntimeException("Transaction reference is required");
        }

        BigDecimal refundAmount = calculateRefundAmount(request);
        ReturnRequest.ReturnStatus before = request.getStatus();

        request.setStatus(ReturnRequest.ReturnStatus.REFUNDED);
        request.setResolvedAt(LocalDateTime.now());
        ReturnRequest saved = returnRequestRepo.save(request);

        logTransaction(
                saved,
                ReturnRequestTransaction.Action.REFUNDED,
                before,
                saved.getStatus(),
                refundAmount,
                "BANK_TRANSFER",
                dto.getTransactionReference().trim(),
                dto.getNote() != null ? dto.getNote().trim() : null
        );

        UserAccount user = request.getOrderItem().getOrder().getUser();
        notificationService.createNotification(
                user,
                "Refund Completed",
                "We have transferred your refund. Please check your bank account and confirm after receiving the money.",
                "RETURN",
                request.getRequestId()
        );

        return saved;
    }

    @Transactional
    public ReturnRequest confirmRefundReceivedByCustomer(Long requestId) {
        ReturnRequest request = returnRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Return request not found"));

        UserAccount currentUser = getCurrentUser();
        OrderItem orderItem = request.getOrderItem();

        if (orderItem == null || orderItem.getOrder() == null || orderItem.getOrder().getUser() == null
                || !orderItem.getOrder().getUser().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("You are not allowed to confirm this refund");
        }

        if (request.getStatus() != ReturnRequest.ReturnStatus.REFUNDED) {
            throw new RuntimeException("Only refunded requests can be confirmed by customer");
        }

        ReturnRequest.ReturnStatus before = request.getStatus();
        request.setStatus(ReturnRequest.ReturnStatus.COMPLETED);
        request.setResolvedAt(LocalDateTime.now());
        ReturnRequest saved = returnRequestRepo.save(request);

        logTransaction(saved, ReturnRequestTransaction.Action.CUSTOMER_CONFIRMED_REFUND_RECEIVED, before, saved.getStatus(), null, null, null, null);

        notificationService.notifyAdmins(
                "Customer Confirmed Refund",
                "Customer has confirmed receiving the refund for request #" + requestId,
                "RETURN",
                requestId
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
                    "Not enough stock for exchange. Available: " + currentStock + ", requested: " + requestedQty
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

        ReturnRequest.ReturnStatus before = request.getStatus();
        request.setStatus(ReturnRequest.ReturnStatus.REJECTED);
        request.setRejectionReason(rejectionReason.trim());
        request.setResolvedAt(LocalDateTime.now());
        ReturnRequest saved = returnRequestRepo.save(request);

        logTransaction(saved, ReturnRequestTransaction.Action.REJECTED, before, saved.getStatus(), null, null, null, rejectionReason.trim());

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

        if (request.getRequestType() == ReturnRequest.RequestType.EXCHANGE) {
            if (request.getStatus() != ReturnRequest.ReturnStatus.APPROVED) {
                throw new RuntimeException("Only approved exchange requests can be completed");
            }

            ReturnRequest.ReturnStatus before = request.getStatus();
            request.setStatus(ReturnRequest.ReturnStatus.COMPLETED);
            request.setResolvedAt(LocalDateTime.now());
            ReturnRequest saved = returnRequestRepo.save(request);

            logTransaction(saved, ReturnRequestTransaction.Action.COMPLETED, before, saved.getStatus(), null, null, null, null);

            UserAccount user = request.getOrderItem().getOrder().getUser();
            notificationService.createNotification(
                    user,
                    "Exchange Process Completed",
                    "Your exchange process for " + request.getOrderItem().getProductName() + " is now complete.",
                    "RETURN",
                    request.getRequestId()
            );

            return saved;
        }

        return confirmRefundReceivedByCustomer(requestId);
    }

    public ReturnRequestResponseDTO mapToDTO(ReturnRequest request) {
        OrderItem orderItem = request.getOrderItem();
        BigDecimal refundAmount = request.getRequestType() == ReturnRequest.RequestType.RETURN
                ? calculateRefundAmount(request)
                : null;

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
                .refundAmount(refundAmount)
                .transactions(getTransactionDTOs(request.getRequestId()))
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
                        .fullName(oldOrder.getFullName())
                        .phone(oldOrder.getPhone())
                        .address(oldOrder.getAddress())
                        .note(oldOrder.getNote())
                        .shippingAddress(oldOrder.getShippingAddress())
                        .billingAddress(oldOrder.getBillingAddress())
                        .totalPrice(BigDecimal.ZERO)
                        .shippingFee(BigDecimal.ZERO)
                        .voucherDiscount(BigDecimal.ZERO)
                        .finalPrice(BigDecimal.ZERO)
                        .depositAmount(BigDecimal.ZERO)
                        .depositType("FULL")
                        .depositPaymentMethod("EXCHANGE")
                        .remainingPaymentStage("PAID")
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

    private List<ReturnRequestTransactionDTO> getTransactionDTOs(Long requestId) {
        return returnRequestTransactionRepository.findByReturnRequestRequestIdOrderByCreatedAtAsc(requestId)
                .stream()
                .map(tx -> ReturnRequestTransactionDTO.builder()
                        .transactionId(tx.getTransactionId())
                        .action(tx.getAction() != null ? tx.getAction().name() : null)
                        .statusBefore(tx.getStatusBefore() != null ? tx.getStatusBefore().name() : null)
                        .statusAfter(tx.getStatusAfter() != null ? tx.getStatusAfter().name() : null)
                        .amount(tx.getAmount())
                        .paymentMethod(tx.getPaymentMethod())
                        .transactionReference(tx.getTransactionReference())
                        .note(tx.getNote())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .toList();
    }

    private void logTransaction(
            ReturnRequest request,
            ReturnRequestTransaction.Action action,
            ReturnRequest.ReturnStatus statusBefore,
            ReturnRequest.ReturnStatus statusAfter,
            BigDecimal amount,
            String paymentMethod,
            String transactionReference,
            String note
    ) {
        returnRequestTransactionRepository.save(
                ReturnRequestTransaction.builder()
                        .returnRequest(request)
                        .action(action)
                        .statusBefore(statusBefore)
                        .statusAfter(statusAfter)
                        .amount(amount)
                        .paymentMethod(paymentMethod)
                        .transactionReference(transactionReference)
                        .note(note)
                        .build()
        );
    }
}
