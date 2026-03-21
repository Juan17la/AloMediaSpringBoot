package com.peciatech.alomediabackend.notification;

public interface NotificationObservable {
    void addObserver(NotificationObserver observer);
    void removeObserver(NotificationObserver observer);
    void notifyObservers(NotificationEvent event);
}
