package com.peciatech.alomediabackend.user.dto;

import com.peciatech.alomediabackend.auth.dto.response.AuthResponse;
import com.peciatech.alomediabackend.user.dto.response.UserResponse;
import com.peciatech.alomediabackend.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(User user);

    AuthResponse toAuthResponse(User user);
}
