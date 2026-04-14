package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.Notification;
import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.entity.dto.NotificationDTO;
import com.fpt.glasseshop.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final com.fpt.glasseshop.repository.UserAccountRepository userAccountRepository;

    public void createNotification(UserAccount user, String title, String message, String type, Long referenceId) {
        if (user == null) return;
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(referenceId)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    public void notifyAdmins(String title, String message, String type, Long referenceId) {
        List<UserAccount> admins = userAccountRepository.findByRoleIgnoreCase("ADMIN");
        List<UserAccount> staff = userAccountRepository.findByRoleIgnoreCase("OPERATIONAL_STAFF");
        
        java.util.List<UserAccount> recipients = new java.util.ArrayList<>(admins);
        recipients.addAll(staff);

        for (UserAccount person : recipients) {
            createNotification(person, title, message, type, referenceId);
        }
    }

    public List<NotificationDTO> getNotificationsByUser(Long userId) {
        return notificationRepository.findByUserUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserUserIdAndIsReadFalse(userId);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void clearNotifications(Long userId) {
        notificationRepository.deleteByUserUserId(userId);
    }

    private NotificationDTO convertToDTO(Notification n) {
        return NotificationDTO.builder()
                .notificationId(n.getNotificationId())
                .userId(n.getUser() != null ? n.getUser().getUserId() : null)
                .title(n.getTitle())
                .message(n.getMessage())
                .isRead(n.isRead())
                .type(n.getType())
                .referenceId(n.getReferenceId())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
