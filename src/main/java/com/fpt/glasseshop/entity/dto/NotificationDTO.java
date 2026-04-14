package com.fpt.glasseshop.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private Long notificationId;
    private Long userId;
    private String title;
    private String message;
    private boolean isRead;
    private String type;
    private Long referenceId;
    private LocalDateTime createdAt;
}
