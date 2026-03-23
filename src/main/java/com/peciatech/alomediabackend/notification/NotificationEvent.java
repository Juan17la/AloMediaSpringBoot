package com.peciatech.alomediabackend.notification;

import java.time.LocalDateTime;

public class NotificationEvent {

    private final String type;
    private final Long projectId;
    private final String sharedByUserEmail;
    private final String sharedWithUserEmail;
    private final LocalDateTime occurredAt;

    public NotificationEvent(String type, Long projectId, String sharedByUserEmail,
                             String sharedWithUserEmail, LocalDateTime occurredAt) {
        this.type = type;
        this.projectId = projectId;
        this.sharedByUserEmail = sharedByUserEmail;
        this.sharedWithUserEmail = sharedWithUserEmail;
        this.occurredAt = occurredAt;
    }

    public String getType() {
        return type;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getSharedByUserEmail() {
        return sharedByUserEmail;
    }

    public String getSharedWithUserEmail() {
        return sharedWithUserEmail;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
