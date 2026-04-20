package com.peciatech.alomediabackend.project.service;

import com.peciatech.alomediabackend.project.builder.ProjectBuilder;
import com.peciatech.alomediabackend.project.dto.request.CreateProjectRequest;
import com.peciatech.alomediabackend.project.dto.request.UpdateProjectRequest;
import com.peciatech.alomediabackend.project.dto.response.ProjectResponse;
import com.peciatech.alomediabackend.project.entity.Project;
import com.peciatech.alomediabackend.project.history.ProjectHistoryRepository;
import com.peciatech.alomediabackend.project.history.ProjectHistoryService;
import com.peciatech.alomediabackend.project.history.command.CreateProjectHistoryCommand;
import com.peciatech.alomediabackend.project.history.command.EditProjectHistoryCommand;
import com.peciatech.alomediabackend.project.media.ProjectMediaSyncService;
import com.peciatech.alomediabackend.project.media.StorageBinaryResource;
import com.peciatech.alomediabackend.project.repository.ProjectRepository;
import com.peciatech.alomediabackend.project.repository.ProjectShareRepository;
import com.peciatech.alomediabackend.user.entity.User;
import com.peciatech.alomediabackend.user.repository.UserRepository;
import com.peciatech.alomediabackend.common.exception.ProjectNotFoundException;
import com.peciatech.alomediabackend.common.exception.UserNotFoundException;
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
    private final ProjectHistoryService projectHistoryService;
    private final ProjectHistoryRepository projectHistoryRepository;
    private final ProjectMediaSyncService projectMediaSyncService;
    private final ProjectShareRepository projectShareRepository;

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, String requesterEmail) {
        User owner = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + requesterEmail));

        Project project = new ProjectBuilder()
                .setName(request.getName())
                .setTimelineData(request.getTimelineData())
                .setOwner(owner)
                .build();

        Project saved = projectRepository.save(project);
        String syncedTimeline = projectMediaSyncService.syncOnSave(saved, null, saved.getTimelineData());
        saved.setTimelineData(syncedTimeline);
        saved = projectRepository.save(saved);
        projectHistoryService.executeCommand(
                new CreateProjectHistoryCommand(saved.getId(), owner.getId(), null, projectHistoryRepository));
        return toResponse(saved);
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

        ProjectResponse response = toResponse(project);
        response.setTimelineData(projectMediaSyncService.enrichTimelineWithDeliveryUrls(project, project.getTimelineData()));
        return response;
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> listOwnedProjects(String requesterEmail, Pageable pageable) {
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
            String syncedTimeline = projectMediaSyncService.syncOnSave(
                    project,
                    project.getTimelineData(),
                    request.getTimelineData()
            );
            project.setTimelineData(syncedTimeline);
        }
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }

        Project saved = projectRepository.save(project);
        projectHistoryService.executeCommand(
                new EditProjectHistoryCommand(projectId, user.getId(), null, projectHistoryRepository));
        return toResponse(saved);
    }

    @Transactional
    public void deleteProject(Long projectId, String requesterEmail) {
        User user = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + requesterEmail));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (!project.getOwner().getId().equals(user.getId())) {
            throw new ProjectNotFoundException(projectId);
        }

        projectMediaSyncService.deleteAllProjectMedia(project.getTimelineData());
        projectShareRepository.deleteByProjectId(projectId);
        projectHistoryRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
    }

    @Transactional(readOnly = true)
    public StorageBinaryResource getProjectMedia(Long projectId, String mediaId, String requesterEmail) {
        User user = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + requesterEmail));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (!project.getOwner().getId().equals(user.getId())) {
            throw new ProjectNotFoundException(projectId);
        }

        return projectMediaSyncService.loadMediaForProject(project, mediaId);
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
