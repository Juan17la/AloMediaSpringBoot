package com.peciatech.alomediabackend.project.history;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectHistoryResponse {

    private Long id;
    private Long projectId;
    private String eventType;
    private String timelineSnapshot;
    private Long authorUserId;
    private LocalDateTime createdAt;
}
