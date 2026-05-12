package com.peciatech.alomediabackend.project.repository;

import com.peciatech.alomediabackend.project.entity.ProjectMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMediaRepository extends JpaRepository<ProjectMedia, Long> {

    Optional<ProjectMedia> findByProjectId(Long projectId);

    @Query("SELECT pm FROM ProjectMedia pm JOIN FETCH pm.project WHERE pm.project.id IN :projectIds")
    List<ProjectMedia> findByProjectIdIn(@Param("projectIds") List<Long> projectIds);

    void deleteByProjectId(Long projectId);
}
