package com.peciatech.alomediabackend.project.repository;

import com.peciatech.alomediabackend.project.entity.ProjectTracks;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectTracksRepository extends JpaRepository<ProjectTracks, Long> {

    Optional<ProjectTracks> findByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);
}
