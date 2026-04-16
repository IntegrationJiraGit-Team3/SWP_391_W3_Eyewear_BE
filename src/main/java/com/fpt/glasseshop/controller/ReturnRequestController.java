package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.entity.ReturnRequest;
import com.fpt.glasseshop.entity.dto.*;
import com.fpt.glasseshop.service.ReturnRequestService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/return-requests")
public class ReturnRequestController {

    private final ReturnRequestService returnRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReturnRequestResponseDTO>> createReturnRequest(
            @RequestBody ReturnRequestDTO dto) throws BadRequestException {

        ReturnRequestResponseDTO saved = returnRequestService.createReturnRequest(dto);
        return ResponseEntity.ok(ApiResponse.success("Return request created successfully", saved));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReturnRequestResponseDTO>>> getAllReturnRequests() {
        List<ReturnRequestResponseDTO> list = returnRequestService.getAll();
        return ResponseEntity.ok(ApiResponse.success("Return requests fetched successfully", list));
    }

    @PutMapping("/{requestId}/approve")
    public ResponseEntity<ApiResponse<ReturnRequestResponseDTO>> approve(@PathVariable Long requestId) {
        ReturnRequest result = returnRequestService.approveRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success(returnRequestService.mapToDTO(result)));
    }

    @PutMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<ReturnRequestResponseDTO>> reject(
            @PathVariable Long requestId,
            @RequestBody UpdateReturnStatusDTO dto
    ) {
        ReturnRequest result = returnRequestService.rejectRequest(requestId, dto.getRejectionReason());
        return ResponseEntity.ok(ApiResponse.success(returnRequestService.mapToDTO(result)));
    }

    @PutMapping("/{requestId}/received")
    public ResponseEntity<ApiResponse<ReturnRequestResponseDTO>> markReceived(@PathVariable Long requestId) {
        ReturnRequest result = returnRequestService.markAsReceivedReturn(requestId);
        return ResponseEntity.ok(ApiResponse.success(returnRequestService.mapToDTO(result)));
    }

    @PutMapping("/{requestId}/refund-pending")
    public ResponseEntity<ApiResponse<ReturnRequestResponseDTO>> markRefundPending(@PathVariable Long requestId) {
        ReturnRequest result = returnRequestService.markRefundPending(requestId);
        return ResponseEntity.ok(ApiResponse.success(returnRequestService.mapToDTO(result)));
    }

    @PutMapping("/{requestId}/refund-invalid")
    public ResponseEntity<ApiResponse<ReturnRequestResponseDTO>> markRefundInvalid(
            @PathVariable Long requestId,
            @RequestBody UpdateReturnStatusDTO dto
    ) {
        ReturnRequest result = returnRequestService.markRefundInfoInvalid(requestId, dto.getRefundNote());
        return ResponseEntity.ok(ApiResponse.success(returnRequestService.mapToDTO(result)));
    }

    @PutMapping("/{requestId}/refund-info")
    public ResponseEntity<ApiResponse<ReturnRequestResponseDTO>> updateRefundInfo(
            @PathVariable Long requestId,
            @RequestBody UpdateRefundBankInfoDTO dto
    ) {
        ReturnRequestResponseDTO result = returnRequestService.updateRefundBankInfo(requestId, dto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{requestId}/refunded")
    public ResponseEntity<ApiResponse<ReturnRequestResponseDTO>> refunded(
            @PathVariable Long requestId,
            @RequestBody RefundProcessDTO dto
    ) {
        ReturnRequest result = returnRequestService.markAsRefunded(requestId, dto);
        return ResponseEntity.ok(ApiResponse.success(returnRequestService.mapToDTO(result)));
    }

    @PutMapping("/{requestId}/confirm-refund-received")
    public ResponseEntity<ApiResponse<ReturnRequestResponseDTO>> confirmRefundReceived(@PathVariable Long requestId) {
        ReturnRequest result = returnRequestService.confirmRefundReceivedByCustomer(requestId);
        return ResponseEntity.ok(ApiResponse.success(returnRequestService.mapToDTO(result)));
    }

    @PutMapping("/{requestId}/complete")
    public ResponseEntity<ApiResponse<ReturnRequestResponseDTO>> complete(@PathVariable Long requestId) {
        ReturnRequest result = returnRequestService.completeRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success(returnRequestService.mapToDTO(result)));
    }

    @GetMapping("/order-item/{orderItemId}")
    public ResponseEntity<ApiResponse<List<ReturnRequestResponseDTO>>> getByOrderItemId(
            @PathVariable Long orderItemId) {

        List<ReturnRequestResponseDTO> dto = returnRequestService.getByOrderItemId(orderItemId);
        return ResponseEntity.ok(ApiResponse.success("Return request fetched successfully", dto));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<ReturnRequestResponseDTO>> getById(
            @PathVariable Long requestId) {

        ReturnRequestResponseDTO dto = returnRequestService.getById(requestId);
        return ResponseEntity.ok(ApiResponse.success("Return request fetched successfully", dto));
    }
}