package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.Order;
import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.entity.OrderItem;
import com.fpt.glasseshop.entity.dto.CreateOrderRequest;
import com.fpt.glasseshop.entity.dto.OrderRefundRequestDTO;
import com.fpt.glasseshop.entity.dto.OrderDTO;
import com.fpt.glasseshop.entity.dto.RefundProcessDTO;
import com.fpt.glasseshop.entity.dto.VNPayRefundResult;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    Order saveOrder(Order order);

    List<Order> getOrdersByUserId(Long userId);

    List<OrderDTO> getOrdersDTOByUserId(Long userId);

    List<OrderDTO> getAllOrdersDTO();

    Optional<Order> getOrderById(Long orderId);

    Optional<OrderDTO> getOrderDTOById(Long orderId);

    OrderDTO updateOrderStatus(Long orderId, String newStatus);

    OrderDTO updatePaymentOrderStatus(Long orderId, String newStatus);

    OrderDTO updatePaymentMethod(Long orderId, String newMethod);

    OrderDTO cancelPendingPayment(Long orderId);

    VNPayRefundResult refundVnpayForCancelledOrder(Long orderId, String requesterEmail, String reason);

    OrderDTO requestRefundForCancelledOrder(Long orderId, String requesterEmail, OrderRefundRequestDTO dto);

    OrderDTO requestVnpayRefundForCancelledOrder(Long orderId, String requesterEmail, String reason);

    OrderDTO confirmRefundedForCancelledOrder(Long orderId, String confirmerEmail, RefundProcessDTO dto);

<<<<<<< HEAD
=======
    OrderDTO approvePreorder(Long orderId);

    OrderDTO payRemainingBalance(Long orderId);

>>>>>>> 11b4883fe842d92dc5039ae774e757cbb315cd32
    void deleteOrder(Long id);

    OrderDTO createOrderFromCart(UserAccount user, CreateOrderRequest request);

    List<OrderItem> getOrderItems(Long orderId);

    long getTotalCustomersPaid();

    long getTotalOrdersPaid();
}