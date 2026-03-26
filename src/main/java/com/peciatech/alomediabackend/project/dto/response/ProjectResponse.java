package com.peciatech.alomediabackend.project.dto.response;

import com.peciatech.alomediabackend.project.enums.ProjectStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private Long id;

    private String name;

    private ProjectStatus status;

    private String timelineData;

    private Long ownerId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
