package com.peciatech.alomediabackend.common.exception;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(Long id) {
        super("Project not found: " + id);
    }
}
