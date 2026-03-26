package com.peciatech.alomediabackend.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {

    @NotBlank
    private String name;

    private String timelineData;
}
