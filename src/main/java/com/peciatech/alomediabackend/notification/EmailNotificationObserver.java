package com.peciatech.alomediabackend.notification;

import com.peciatech.alomediabackend.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailNotificationObserver implements NotificationObserver {

    private final EmailService emailService;

    @Override
    public void onNotify(NotificationEvent event) {
        String message = "A project was shared with you by "
                + event.getSharedByUserEmail() + ".";

        emailService.sendEmail(
                event.getSharedWithUserEmail(),
                event.getSharedByUserEmail() + " has shared a project with you",
                message
        );
    }
}
