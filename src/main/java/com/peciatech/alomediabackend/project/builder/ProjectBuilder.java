package com.peciatech.alomediabackend.project.builder;

import com.peciatech.alomediabackend.project.entity.Project;
import com.peciatech.alomediabackend.project.enums.ProjectStatus;
import com.peciatech.alomediabackend.user.entity.User;

public class ProjectBuilder {

    private String name;
    private String description;
    private String timelineData;
    private User owner;

    public ProjectBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public ProjectBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public ProjectBuilder setTimelineData(String timelineData) {
        this.timelineData = timelineData;
        return this;
    }

    public ProjectBuilder setOwner(User owner) {
        this.owner = owner;
        return this;
    }

    public Project build() {
        Project project = new Project();
        project.setName(name);
        project.setTimelineData(timelineData != null ? timelineData : "{\"version\":1,\"tracks\":[],\"media\":[]}");
        project.setOwner(owner);
        project.setStatus(ProjectStatus.DRAFT);
        return project;
    }
}
