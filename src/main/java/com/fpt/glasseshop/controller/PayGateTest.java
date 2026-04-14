package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.config.utils;
import org.springframework.stereotype.Controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class PayGateTest {
    String vnp_Version = "2.1.0";
    String vnp_Command = "pay";
    String vnp_OrderInfo = "TESTING PAY";
    String orderType = "other";
    String vnp_TxnRef = "MIEU TA TEST22";
    String vnp_IpAddr = "171.225.193.29";
    String vnp_TmnCode = "8CVCL0QE";
    String vnp_amount = "1000000";
    String vnp_Locale = "vn";
    String vnp_CurrCode = "VND";

    String returnUrl = "http://localhost:8080/api/v1/payment/vnpay/return";

    Map<String, String> vnp_Params = new HashMap<>();

    String vnp_Url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    public String Pay() throws UnsupportedEncodingException {
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", vnp_amount);
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        String bank_code = "";
        if (!bank_code.isEmpty()) {
            vnp_Params.put("vnp_BankCode", bank_code);
        }
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Amount", vnp_amount);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        vnp_Params.put("vnp_CreateDate", cld.getTime().toString());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = utils.hmacSHA512("FSLP4TXRVF5MJHQ3QYAPGFWCYH3NOZOP", hashData.toString());
        queryUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?" + queryUrl + "&vnp_SecureHash="
                + vnp_SecureHash;
        return queryUrl;

    }

    static void main() throws UnsupportedEncodingException {
        PayGateTest payGateTest = new PayGateTest();
        try {
            System.out.println(payGateTest.Pay());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
