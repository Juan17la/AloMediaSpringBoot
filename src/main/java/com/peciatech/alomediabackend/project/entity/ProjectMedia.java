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
@Table(name = "project_media", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Lob
    @Column(name = "media_data", columnDefinition = "TEXT", nullable = false)
    private String mediaData;

    @Column(name = "media_hash", nullable = false, length = 64)
    private String mediaHash;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
