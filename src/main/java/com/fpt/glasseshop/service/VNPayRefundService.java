package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.Order;
import com.fpt.glasseshop.entity.Payment;
import com.fpt.glasseshop.entity.dto.VNPayRefundResult;

public interface VNPayRefundService {
    VNPayRefundResult refund(Order order, Payment payment, String requestedBy, String reason);
}