package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.entity.dto.ApiResponse;
import com.fpt.glasseshop.entity.dto.NotificationDTO;
import com.fpt.glasseshop.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "NotificationAPI", description = "Operations related to user notifications")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get notifications for a user")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotificationsByUser(userId)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PatchMapping("/user/{userId}/read-all")
    @Operation(summary = "Mark all notifications for a user as read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @DeleteMapping("/user/{userId}")
    @Operation(summary = "Clear all notifications for a user")
    public ResponseEntity<ApiResponse<Void>> clearNotifications(@PathVariable Long userId) {
        notificationService.clearNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications cleared", null));
    }
}
