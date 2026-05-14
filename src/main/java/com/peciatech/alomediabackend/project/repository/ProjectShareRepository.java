package com.peciatech.alomediabackend.project.repository;

import com.peciatech.alomediabackend.project.dto.response.ProjectSummaryResponse;
import com.peciatech.alomediabackend.project.entity.Project;
import com.peciatech.alomediabackend.project.entity.ProjectShare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectShareRepository extends JpaRepository<ProjectShare, Long> {

    boolean existsByProjectIdAndSharedWithId(Long projectId, Long sharedWithId);

    long deleteByProjectId(Long projectId);

    @Query("SELECT ps.project FROM ProjectShare ps WHERE ps.sharedWith.id = :userId")
    Page<Project> findSharedProjectsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT new com.peciatech.alomediabackend.project.dto.response.ProjectSummaryResponse(" +
           "p.id, p.name, p.status, p.owner.id, p.createdAt, p.updatedAt) " +
           "FROM ProjectShare ps JOIN ps.project p WHERE ps.sharedWith.id = :userId")
    Page<ProjectSummaryResponse> findSharedProjectSummariesByUserId(@Param("userId") Long userId, Pageable pageable);
}
