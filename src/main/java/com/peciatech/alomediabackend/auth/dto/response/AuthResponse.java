package com.peciatech.alomediabackend.auth.dto.response;

import com.peciatech.alomediabackend.user.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
}
