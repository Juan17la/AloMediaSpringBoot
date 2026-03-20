package com.peciatech.alomediabackend.user.dto.response;

import com.peciatech.alomediabackend.user.enums.Role;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
}
