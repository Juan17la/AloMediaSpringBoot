package com.peciatech.alomediabackend.project.repository;

import com.peciatech.alomediabackend.project.dto.response.ProjectSummaryResponse;
import com.peciatech.alomediabackend.project.entity.Project;
import com.peciatech.alomediabackend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOwnerId(Long ownerId);

    Page<Project> findByOwner(User owner, Pageable pageable);

    Page<Project> findByOwnerId(Long ownerId, Pageable pageable);

    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    @Query("SELECT new com.peciatech.alomediabackend.project.dto.response.ProjectSummaryResponse(" +
           "p.id, p.name, p.status, p.owner.id, p.createdAt, p.updatedAt) " +
           "FROM Project p WHERE p.owner.id = :ownerId")
    Page<ProjectSummaryResponse> findSummariesByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);
}
