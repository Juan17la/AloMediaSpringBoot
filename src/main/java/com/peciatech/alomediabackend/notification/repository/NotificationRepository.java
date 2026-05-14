package com.peciatech.alomediabackend.notification.repository;

import com.peciatech.alomediabackend.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientEmailOrderByCreatedAtDesc(String email, Pageable pageable);
    List<Notification> findByRecipientEmailAndReadFalse(String email);
}
