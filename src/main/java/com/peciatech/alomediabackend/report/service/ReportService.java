package com.peciatech.alomediabackend.report.service;

import com.peciatech.alomediabackend.project.history.EventType;
import com.peciatech.alomediabackend.project.history.ProjectHistoryRepository;
import com.peciatech.alomediabackend.project.repository.ProjectRepository;
import com.peciatech.alomediabackend.report.ReportFactory;
import com.peciatech.alomediabackend.report.ReportFactoryProvider;
import com.peciatech.alomediabackend.report.dto.response.ReportData;
import com.peciatech.alomediabackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectHistoryRepository projectHistoryRepository;
    private final ReportFactoryProvider reportFactoryProvider;

    public Object generateReport(String format) {
        long totalUsers = userRepository.count();
        long totalProjects = projectRepository.count();
        long totalProjectsCreated = projectHistoryRepository.countByEventType(EventType.CREATE);
        long totalProjectsEdited = projectHistoryRepository.countByEventType(EventType.EDIT);
        long totalProjectsExported = projectHistoryRepository.countByEventType(EventType.EXPORT);
        long totalProjectsShared = projectHistoryRepository.countByEventType(EventType.SHARE);

        ReportData data = ReportData.builder()
                .totalUsers(totalUsers)
                .totalProjects(totalProjects)
                .totalProjectsCreated(totalProjectsCreated)
                .totalProjectsEdited(totalProjectsEdited)
                .totalProjectsExported(totalProjectsExported)
                .totalProjectsShared(totalProjectsShared)
                .generatedAt(LocalDateTime.now())
                .build();

        ReportFactory factory = reportFactoryProvider.getFactory(format);
        return factory.generateReport(data);
    }
}
