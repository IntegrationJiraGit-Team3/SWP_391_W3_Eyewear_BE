package com.fpt.glasseshop.entity.dto;

import com.fpt.glasseshop.entity.OrderItemServiceRequest.RequestType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitServiceRequest {
    @NotNull(message = "Order Item ID is required")
    private Long orderItemId;

    @NotNull(message = "Request type is required")
    private RequestType requestType;

    private String reason;
}
