package com.peciatech.alomediabackend.project.controller;

import com.peciatech.alomediabackend.project.dto.request.CreateProjectRequest;
import com.peciatech.alomediabackend.project.dto.request.ShareProjectRequest;
import com.peciatech.alomediabackend.project.dto.request.UpdateProjectRequest;
import com.peciatech.alomediabackend.project.dto.response.ProjectResponse;
import com.peciatech.alomediabackend.project.service.ProjectService;
import com.peciatech.alomediabackend.project.service.ProjectSharingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectSharingService projectSharingService;

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(request, userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.getProject(id, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<Page<ProjectResponse>> listOwnedProjects(
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.listOwnedProjects(userDetails.getUsername(), pageable));
    }

    @GetMapping("/shared")
    public ResponseEntity<Page<ProjectResponse>> listSharedProjects(
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectSharingService.listSharedProjects(userDetails.getUsername(), pageable));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.updateProject(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        projectService.deleteProject(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<Void> shareProject(
            @PathVariable Long id,
            @Valid @RequestBody ShareProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        projectSharingService.shareProject(id, userDetails.getUsername(), request.getSharedWithEmail());
        return ResponseEntity.ok().build();
    }
}
