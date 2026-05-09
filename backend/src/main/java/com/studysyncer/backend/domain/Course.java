package com.studysyncer.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "courses", indexes = {
        @Index(name = "idx_courses_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ColorKey colorKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ColorVariant colorVariant;

    @Column(nullable = false)
    private Integer displayOrder;

    private String section;
    private String term;
    private String professor;
    private String room;
    private Integer credits;

    @Column(columnDefinition = "TEXT")
    private String description;

    public String getSlug() {
        return code == null ? null : code.replace(' ', '-');
    }
}
