package com.fpt.glasseshop.service.impl;

import com.fpt.glasseshop.config.VNPayConfig;
import com.fpt.glasseshop.config.utils;
import com.fpt.glasseshop.entity.Order;
import com.fpt.glasseshop.entity.Payment;
import com.fpt.glasseshop.entity.dto.VNPayRefundResult;
import com.fpt.glasseshop.service.VNPayRefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VNPayRefundServiceImpl implements VNPayRefundService {

    private final VNPayConfig vnPayConfig;

    @Override
    public VNPayRefundResult refund(Order order, Payment payment, String requestedBy, String reason) {
        try {
            if (vnPayConfig.isRefundSimulate()) {
                String txnRef = payment != null ? payment.getTransactionReference() : null;
                String requestId = "SIM" + System.currentTimeMillis();
                return VNPayRefundResult.builder()
                        .success(true)
                        .refundStatus("REFUNDED")
                        .responseCode("00")
                        .message("SIMULATED")
                        .requestId(requestId)
                        .transactionReference(txnRef)
                        .rawResponse("SIMULATED_REFUND")
                        .build();
            }

            String requestId = "RF" + System.currentTimeMillis();
            String createDate = nowGmt7();
            String txnRef = payment.getTransactionReference();
            String transactionDate = formatPaidAtGmt7(payment);
            long amount = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

            Map<String, String> params = new HashMap<>();
            params.put("vnp_RequestId", requestId);
            params.put("vnp_Version", vnPayConfig.getVersion());
            params.put("vnp_Command", "refund");
            params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
            params.put("vnp_TransactionType", "02"); // full refund
            params.put("vnp_TxnRef", txnRef);
            params.put("vnp_Amount", String.valueOf(amount));
            params.put("vnp_OrderInfo", reason != null && !reason.isBlank() ? reason : "Refund order " + order.getOrderCode());
            params.put("vnp_TransactionDate", transactionDate);
            params.put("vnp_CreateBy", requestedBy != null ? requestedBy : "system");
            params.put("vnp_CreateDate", createDate);
            params.put("vnp_IpAddr", "127.0.0.1");

            String hashData = buildHashData(params);
            String secureHash = utils.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
            params.put("vnp_SecureHash", secureHash);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(params, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.exchange(
                    vnPayConfig.getApiUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            String body = response.getBody() == null ? "" : response.getBody();

            boolean success =
                    body.contains("\"vnp_ResponseCode\":\"00\"")
                    || body.contains("\"vnp_ResponseCode\":\"07\"");

            return VNPayRefundResult.builder()
                    .success(success)
                    .refundStatus(success ? "REFUNDED" : "FAILED")
                    .responseCode(extractValue(body, "vnp_ResponseCode"))
                    .message(success ? "Refund request accepted by VNPay" : "VNPay refund failed")
                    .requestId(requestId)
                    .transactionReference(txnRef)
                    .rawResponse(body)
                    .build();

        } catch (Exception e) {
            return VNPayRefundResult.builder()
                    .success(false)
                    .refundStatus("FAILED")
                    .message("Refund call error: " + e.getMessage())
                    .rawResponse(e.toString())
                    .build();
        }
    }

    private String nowGmt7() {
        // VNPay expects yyyyMMddHHmmss in GMT+7.
        // Our app runs in local timezone; using LocalDateTime avoids Etc/GMT sign confusion.
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String formatPaidAtGmt7(Payment payment) {
        LocalDateTime paidAt = payment.getPaidAt() != null ? payment.getPaidAt() : LocalDateTime.now();
        return paidAt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String buildHashData(Map<String, String> params) {
        // IMPORTANT: VNPay refund signature uses a FIXED field order.
        // Sorting keys alphabetically will generate an invalid signature (responseCode=97).
        String[] orderedKeys = {
                "vnp_RequestId",
                "vnp_Version",
                "vnp_Command",
                "vnp_TmnCode",
                "vnp_TransactionType",
                "vnp_TxnRef",
                "vnp_Amount",
                "vnp_OrderInfo",
                "vnp_TransactionDate",
                "vnp_CreateBy",
                "vnp_CreateDate",
                "vnp_IpAddr"
        };

        StringJoiner joiner = new StringJoiner("|");
        for (String key : orderedKeys) {
            String value = params.get(key);
            joiner.add(value == null ? "" : value);
        }
        return joiner.toString();
    }

    private String extractValue(String json, String key) {
        try {
            String marker = "\"" + key + "\":\"";
            int start = json.indexOf(marker);
            if (start < 0) return null;
            start += marker.length();
            int end = json.indexOf("\"", start);
            if (end < 0) return null;
            return json.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }
}