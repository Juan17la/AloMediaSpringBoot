package com.peciatech.alomediabackend.project.history;

import com.peciatech.alomediabackend.common.exception.ProjectNotFoundException;
import com.peciatech.alomediabackend.common.exception.UnauthorizedException;
import com.peciatech.alomediabackend.common.exception.UserNotFoundException;
import com.peciatech.alomediabackend.project.entity.Project;
import com.peciatech.alomediabackend.project.repository.ProjectRepository;
import com.peciatech.alomediabackend.user.entity.User;
import com.peciatech.alomediabackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectHistoryService {

    private final ProjectHistoryRepository projectHistoryRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectHistoryResponse executeCommand(HistoryCommand command) {
        ProjectHistory history = command.execute();
        return toResponse(history);
    }

    public List<ProjectHistoryResponse> getHistory(Long projectId, String requesterEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        User user = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + requesterEmail));

        if (!project.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("Access denied to project history");
        }

        return projectHistoryRepository.findAllByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ProjectHistoryResponse toResponse(ProjectHistory history) {
        return ProjectHistoryResponse.builder()
                .id(history.getId())
                .projectId(history.getProjectId())
                .eventType(history.getEventType().name())
                .timelineSnapshot(history.getTimelineSnapshot())
                .authorUserId(history.getAuthorUserId())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
