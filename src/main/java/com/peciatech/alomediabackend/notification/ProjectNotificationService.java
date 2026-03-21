package com.peciatech.alomediabackend.notification;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectNotificationService implements NotificationObservable {

    private final EmailNotificationObserver emailNotificationObserver;

    private final List<NotificationObserver> observers = new ArrayList<>();

    @PostConstruct
    public void init() {
        addObserver(emailNotificationObserver);
    }

    @Override
    public void addObserver(NotificationObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(NotificationObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(NotificationEvent event) {
        for (NotificationObserver observer : observers) {
            observer.onNotify(event);
        }
    }

    public void shareProject(Long projectId, String sharedByEmail, String sharedWithEmail) {
        NotificationEvent event = new NotificationEvent(
                "PROJECT_SHARED",
                projectId,
                sharedByEmail,
                sharedWithEmail,
                LocalDateTime.now()
        );
        notifyObservers(event);
    }
}
