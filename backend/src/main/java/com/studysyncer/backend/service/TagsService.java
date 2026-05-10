package com.studysyncer.backend.service;

import com.studysyncer.backend.domain.Tag;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.TagRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagsService {

    private final TagRepository tagRepository;

    public TagsService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional
    public Tag createTag(User user, String rawName) {
        String name = sanitize(rawName);
        if (tagRepository.findByUserAndName(user, name).isPresent()) {
            throw new IllegalArgumentException("Tag '" + name + "' already exists");
        }
        Tag t = new Tag();
        t.setUser(user);
        t.setName(name);
        return tagRepository.save(t);
    }

    @Transactional
    public Tag renameTag(User user, Long id, String newName) {
        Tag t = tagRepository.findById(id)
                .filter(x -> x.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Not your tag"));
        String name = sanitize(newName);
        if (!t.getName().equalsIgnoreCase(name)) {
            tagRepository.findByUserAndName(user, name).ifPresent(other -> {
                throw new IllegalArgumentException("Tag '" + name + "' already exists");
            });
        }
        t.setName(name);
        return tagRepository.save(t);
    }

    @Transactional
    public void deleteTag(User user, Long id) {
        Tag t = tagRepository.findById(id)
                .filter(x -> x.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Not your tag"));
        tagRepository.delete(t);
    }

    private static String sanitize(String raw) {
        if (raw == null) throw new IllegalArgumentException("Tag name required");
        String n = raw.trim().replaceAll("^#+", "");
        if (n.isEmpty()) throw new IllegalArgumentException("Tag name required");
        if (n.length() > 40) n = n.substring(0, 40);
        return n;
    }
}
