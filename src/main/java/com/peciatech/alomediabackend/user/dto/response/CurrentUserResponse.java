package com.peciatech.alomediabackend.user.dto.response;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentUserResponse {

    private boolean authenticated;
    private UserResponse user;
}
