package com.peciatech.alomediabackend.project.repository;

import com.peciatech.alomediabackend.project.entity.Project;
import com.peciatech.alomediabackend.project.entity.ProjectShare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectShareRepository extends JpaRepository<ProjectShare, Long> {

    boolean existsByProjectIdAndSharedWithId(Long projectId, Long sharedWithId);

    @Query("SELECT ps.project FROM ProjectShare ps WHERE ps.sharedWith.id = :userId")
    Page<Project> findSharedProjectsByUserId(@Param("userId") Long userId, Pageable pageable);
}
