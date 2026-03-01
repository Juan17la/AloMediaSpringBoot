package com.peciatech.alomediabackend.dto.response;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryValidationResponse {

    private boolean valid;
}
