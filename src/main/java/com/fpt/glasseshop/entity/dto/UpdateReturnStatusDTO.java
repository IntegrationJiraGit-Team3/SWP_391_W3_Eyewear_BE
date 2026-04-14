package com.fpt.glasseshop.entity.dto;

import com.fpt.glasseshop.entity.ReturnRequest;
import lombok.Data;

@Data
public class UpdateReturnStatusDTO {
    private ReturnRequest.ReturnStatus status;
    private String rejectionReason;
    private String refundNote;
}