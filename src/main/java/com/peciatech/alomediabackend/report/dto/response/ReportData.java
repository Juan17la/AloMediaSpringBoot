package com.peciatech.alomediabackend.report.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportData {

    private long totalUsers;
    private long totalProjects;
    private long totalProjectsCreated;
    private long totalProjectsEdited;
    private long totalProjectsExported;
    private long totalProjectsShared;
    private LocalDateTime generatedAt;
}
