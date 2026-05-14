package com.peciatech.alomediabackend.notification;

import com.peciatech.alomediabackend.notification.entity.Notification;
import com.peciatech.alomediabackend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DbNotificationObserver implements NotificationObserver {

    private final NotificationRepository notificationRepository;

    @Override
    public void onNotify(NotificationEvent event) {
        String message = "A project was shared with you by "
                + event.getSharedByUserEmail() + ".";

        notificationRepository.save(Notification.builder()
                .recipientEmail(event.getSharedWithUserEmail())
                .type(event.getType())
                .message(message)
                .projectId(event.getProjectId())
                .build());
    }
}
