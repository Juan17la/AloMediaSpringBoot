package com.peciatech.alomediabackend.dto.response;

import com.peciatech.alomediabackend.enums.Role;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
}
