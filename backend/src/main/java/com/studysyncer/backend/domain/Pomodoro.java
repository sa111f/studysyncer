package com.studysyncer.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "pomodoros", indexes = {
        @Index(name = "idx_pomodoros_session_id", columnList = "session_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Pomodoro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private StudySession session;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Integer durationSeconds;

    @Column(nullable = false)
    private Boolean completed;

    @Column(name = "planned_duration_seconds", nullable = false)
    private Integer plannedDurationSeconds = 1500;
}
