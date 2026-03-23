package com.peciatech.alomediabackend.project.entity;

import com.peciatech.alomediabackend.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_shares", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "shared_with_id"})
})
@Getter
@NoArgsConstructor
public class ProjectShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_by_id", nullable = false)
    private User sharedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_id", nullable = false)
    private User sharedWith;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime sharedAt;

    public ProjectShare(Project project, User sharedBy, User sharedWith) {
        this.project = project;
        this.sharedBy = sharedBy;
        this.sharedWith = sharedWith;
    }
}
