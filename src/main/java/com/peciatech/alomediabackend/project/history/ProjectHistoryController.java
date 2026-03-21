package com.peciatech.alomediabackend.project.history;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class ProjectHistoryController {

    private final ProjectHistoryService projectHistoryService;

    @GetMapping("/{projectId}")
    public ResponseEntity<List<ProjectHistoryResponse>> getHistory(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectHistoryService.getHistory(projectId, userDetails.getUsername()));
    }
}
