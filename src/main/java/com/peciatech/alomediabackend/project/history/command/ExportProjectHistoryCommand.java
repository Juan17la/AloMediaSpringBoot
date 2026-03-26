package com.peciatech.alomediabackend.project.history.command;

import com.peciatech.alomediabackend.project.history.EventType;
import com.peciatech.alomediabackend.project.history.HistoryCommand;
import com.peciatech.alomediabackend.project.history.ProjectHistory;
import com.peciatech.alomediabackend.project.history.ProjectHistoryRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ExportProjectHistoryCommand implements HistoryCommand {

    private final Long projectId;
    private final Long authorUserId;
    private final String timelineSnapshot;
    private final ProjectHistoryRepository repository;

    @Override
    public ProjectHistory execute() {
        ProjectHistory history = ProjectHistory.builder()
                .projectId(projectId)
                .authorUserId(authorUserId)
                .timelineSnapshot(timelineSnapshot)
                .eventType(EventType.EXPORT)
                .build();
        return repository.save(history);
    }
}
