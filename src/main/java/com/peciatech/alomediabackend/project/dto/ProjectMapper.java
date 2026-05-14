package com.peciatech.alomediabackend.project.dto;

import com.peciatech.alomediabackend.project.dto.response.ProjectResponse;
import com.peciatech.alomediabackend.project.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(source = "owner.id", target = "ownerId")
    ProjectResponse toResponse(Project project);

    default ProjectResponse toResponse(Project project, String timelineData) {
        ProjectResponse response = toResponse(project);
        response.setTimelineData(timelineData);
        return response;
    }
}
