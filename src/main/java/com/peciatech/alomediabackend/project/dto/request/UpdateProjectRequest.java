package com.peciatech.alomediabackend.project.dto.request;

import com.peciatech.alomediabackend.project.enums.ProjectStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectRequest {

    private String name;

    private String description;

    private String timelineData;

    private ProjectStatus status;
}
