package com.peciatech.alomediabackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
}
