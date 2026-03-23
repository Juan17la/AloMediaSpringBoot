package com.peciatech.alomediabackend.project.service;

import com.peciatech.alomediabackend.common.exception.ProjectNotFoundException;
import com.peciatech.alomediabackend.common.exception.UnauthorizedException;
import com.peciatech.alomediabackend.common.exception.UserNotFoundException;
import com.peciatech.alomediabackend.notification.ProjectNotificationService;
import com.peciatech.alomediabackend.project.dto.response.ProjectResponse;
import com.peciatech.alomediabackend.project.entity.Project;
import com.peciatech.alomediabackend.project.entity.ProjectShare;
import com.peciatech.alomediabackend.project.history.ProjectHistoryRepository;
import com.peciatech.alomediabackend.project.history.ProjectHistoryService;
import com.peciatech.alomediabackend.project.history.command.ShareProjectHistoryCommand;
import com.peciatech.alomediabackend.project.repository.ProjectRepository;
import com.peciatech.alomediabackend.project.repository.ProjectShareRepository;
import com.peciatech.alomediabackend.user.entity.User;
import com.peciatech.alomediabackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectSharingService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectShareRepository projectShareRepository;
    private final ProjectNotificationService projectNotificationService;
    private final ProjectHistoryService projectHistoryService;
    private final ProjectHistoryRepository projectHistoryRepository;

    @Transactional
    public void shareProject(Long projectId, String sharedByEmail, String sharedWithEmail) {
        if (sharedByEmail.equals(sharedWithEmail)) {
            throw new IllegalStateException("Cannot share a project with yourself");
        }

        User sharedBy = userRepository.findByEmail(sharedByEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + sharedByEmail));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (!project.getOwner().getId().equals(sharedBy.getId())) {
            throw new UnauthorizedException("You are not the owner of this project");
        }

        User sharedWith = userRepository.findByEmail(sharedWithEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + sharedWithEmail));

        if (projectShareRepository.existsByProjectIdAndSharedWithId(projectId, sharedWith.getId())) {
            throw new IllegalStateException("Project already shared with this user");
        }

        projectShareRepository.save(new ProjectShare(project, sharedBy, sharedWith));

        projectNotificationService.shareProject(projectId, sharedByEmail, sharedWithEmail);

        projectHistoryService.executeCommand(
                new ShareProjectHistoryCommand(projectId, sharedBy.getId(), null, projectHistoryRepository));
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> listSharedProjects(String requesterEmail, Pageable pageable) {
        User user = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + requesterEmail));

        return projectShareRepository.findSharedProjectsByUserId(user.getId(), pageable)
                .map(this::toResponse);
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
