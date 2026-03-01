package com.peciatech.alomediabackend.dto.response;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentUserResponse {

    private boolean authenticated;
    private UserResponse user;
}
