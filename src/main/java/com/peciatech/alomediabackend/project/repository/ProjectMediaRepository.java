package com.peciatech.alomediabackend.project.repository;

import com.peciatech.alomediabackend.project.entity.ProjectMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectMediaRepository extends JpaRepository<ProjectMedia, Long> {

    Optional<ProjectMedia> findByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);
}
