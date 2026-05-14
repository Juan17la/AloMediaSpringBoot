package com.peciatech.alomediabackend.project.dto.response;

import com.peciatech.alomediabackend.project.enums.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSummaryResponse {

    private Long id;

    private String name;

    private ProjectStatus status;

    private Long ownerId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
