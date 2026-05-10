package com.studysyncer.backend.repository;

import com.studysyncer.backend.domain.Tag;
import com.studysyncer.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByUserOrderByNameAsc(User user);

    Optional<Tag> findByUserAndName(User user, String name);

    void deleteByUser(User user);
}
