package com.peciatech.alomediabackend.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareNotificationRequest {

    @NotBlank
    private String sharedWithEmail;

    @NotNull
    private Long projectId;
}
