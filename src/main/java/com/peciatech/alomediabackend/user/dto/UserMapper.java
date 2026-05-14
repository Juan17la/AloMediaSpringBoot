package com.peciatech.alomediabackend.user.dto;

import com.peciatech.alomediabackend.auth.dto.response.AuthResponse;
import com.peciatech.alomediabackend.user.dto.response.UserResponse;
import com.peciatech.alomediabackend.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(User user);

    @Mapping(target = "token", ignore = true)
    AuthResponse toAuthResponse(User user);
}
