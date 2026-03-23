package com.peciatech.alomediabackend.notification.service;

import com.peciatech.alomediabackend.common.exception.ResourceNotFoundException;
import com.peciatech.alomediabackend.common.exception.UnauthorizedException;
import com.peciatech.alomediabackend.notification.dto.response.NotificationResponse;
import com.peciatech.alomediabackend.notification.entity.Notification;
import com.peciatech.alomediabackend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationResponse> getMyNotifications(String email) {
        return notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<NotificationResponse> getUnread(String email) {
        return notificationRepository.findByRecipientEmailAndReadFalse(email)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void markAsRead(Long notificationId, String requesterEmail) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        if (!notification.getRecipientEmail().equals(requesterEmail)) {
            throw new UnauthorizedException("You are not allowed to modify this notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .message(notification.getMessage())
                .read(notification.isRead())
                .projectId(notification.getProjectId())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
