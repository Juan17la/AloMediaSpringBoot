package com.peciatech.alomediabackend.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectHistoryRepository extends JpaRepository<ProjectHistory, Long> {

    List<ProjectHistory> findAllByProjectId(Long projectId);
}
