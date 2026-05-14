package com.peciatech.alomediabackend.project.repository;

import com.peciatech.alomediabackend.project.entity.ProjectTracks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectTracksRepository extends JpaRepository<ProjectTracks, Long> {

    Optional<ProjectTracks> findByProjectId(Long projectId);

    @Query("SELECT pt FROM ProjectTracks pt JOIN FETCH pt.project WHERE pt.project.id IN :projectIds")
    List<ProjectTracks> findByProjectIdIn(@Param("projectIds") List<Long> projectIds);

    void deleteByProjectId(Long projectId);
}
