package com.peciatech.alomediabackend.project.history;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class ProjectHistoryController {

    private final ProjectHistoryService projectHistoryService;

    @GetMapping("/{projectId}")
    public ResponseEntity<Page<ProjectHistoryResponse>> getHistory(
            @PathVariable Long projectId,
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectHistoryService.getHistory(projectId, userDetails.getUsername(), pageable));
    }
}
