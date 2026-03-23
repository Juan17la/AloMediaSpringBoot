package com.peciatech.alomediabackend.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ShareProjectRequest {

    @NotBlank
    private String sharedWithEmail;
}
