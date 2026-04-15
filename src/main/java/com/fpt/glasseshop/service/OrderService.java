package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.Order;
import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.entity.OrderItem;
import com.fpt.glasseshop.entity.dto.CreateOrderRequest;
import com.fpt.glasseshop.entity.dto.OrderDTO;

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

    void deleteOrder(Long id);

    OrderDTO createOrderFromCart(UserAccount user, CreateOrderRequest request);

    List<OrderItem> getOrderItems(Long orderId);

    long getTotalCustomersPaid();

    long getTotalOrdersPaid();
}