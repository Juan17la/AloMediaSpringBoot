package com.peciatech.alomediabackend.auth.dto.response;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryValidationResponse {

    private boolean valid;
}
