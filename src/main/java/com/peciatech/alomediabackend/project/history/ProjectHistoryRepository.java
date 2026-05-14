package com.peciatech.alomediabackend.project.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectHistoryRepository extends JpaRepository<ProjectHistory, Long> {

    List<ProjectHistory> findAllByProjectId(Long projectId);

    Page<ProjectHistory> findAllByProjectId(Long projectId, Pageable pageable);

    long deleteByProjectId(Long projectId);

    long countByEventType(EventType eventType);
}
