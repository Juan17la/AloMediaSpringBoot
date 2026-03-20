package com.peciatech.alomediabackend.mapper;

import com.peciatech.alomediabackend.dto.response.ProjectResponse;
import com.peciatech.alomediabackend.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(source = "owner.id", target = "ownerId")
    ProjectResponse toResponse(Project project);
}
