package com.peciatech.alomediabackend.project.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_tracks", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTracks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Lob
    @Column(name = "tracks_data", columnDefinition = "TEXT", nullable = false)
    private String tracksData;

    @Column(name = "tracks_hash", nullable = false, length = 64)
    private String tracksHash;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
