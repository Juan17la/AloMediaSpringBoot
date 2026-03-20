package com.peciatech.alomediabackend.exception;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(Long id) {
        super("Project not found: " + id);
    }
}
