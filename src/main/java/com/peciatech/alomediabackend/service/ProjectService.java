package com.peciatech.alomediabackend.service;

import com.peciatech.alomediabackend.builder.ProjectBuilder;
import com.peciatech.alomediabackend.dto.request.CreateProjectRequest;
import com.peciatech.alomediabackend.dto.request.UpdateProjectRequest;
import com.peciatech.alomediabackend.dto.response.ProjectResponse;
import com.peciatech.alomediabackend.entity.Project;
import com.peciatech.alomediabackend.entity.User;
import com.peciatech.alomediabackend.exception.ProjectNotFoundException;
import com.peciatech.alomediabackend.exception.UserNotFoundException;
import com.peciatech.alomediabackend.repository.ProjectRepository;
import com.peciatech.alomediabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProjectResponse createProject(CreateProjectRequest request, String requesterEmail) {
        User owner = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + requesterEmail));

        Project project = new ProjectBuilder()
                .setName(request.getName())
                .setTimelineData(request.getTimelineData())
                .setOwner(owner)
                .build();

        return toResponse(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long projectId, String requesterEmail) {
        User user = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + requesterEmail));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (!project.getOwner().getId().equals(user.getId())) {
            throw new ProjectNotFoundException(projectId);
        }

        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> listProjects(String requesterEmail, Pageable pageable) {
        User user = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + requesterEmail));

        return projectRepository.findByOwnerId(user.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, UpdateProjectRequest request, String requesterEmail) {
        User user = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + requesterEmail));

        if (!projectRepository.existsByIdAndOwnerId(projectId, user.getId())) {
            throw new ProjectNotFoundException(projectId);
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getTimelineData() != null) {
            project.setTimelineData(request.getTimelineData());
        }
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }

        return toResponse(projectRepository.save(project));
    }

    public void deleteProject(Long projectId, String requesterEmail) {
        User user = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + requesterEmail));

        if (!projectRepository.existsByIdAndOwnerId(projectId, user.getId())) {
            throw new ProjectNotFoundException(projectId);
        }

        projectRepository.deleteById(projectId);
    }

    private ProjectResponse toResponse(Project project) {
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setName(project.getName());
        response.setStatus(project.getStatus());
        response.setTimelineData(project.getTimelineData());
        response.setOwnerId(project.getOwner().getId());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());
        return response;
    }
}
