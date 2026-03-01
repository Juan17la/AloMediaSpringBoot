package com.peciatech.alomediabackend.mapper;

import com.peciatech.alomediabackend.dto.response.AuthResponse;
import com.peciatech.alomediabackend.dto.response.UserResponse;
import com.peciatech.alomediabackend.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(User user);

    AuthResponse toAuthResponse(User user);
}
