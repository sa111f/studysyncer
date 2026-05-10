package com.studysyncer.backend.web.api;

import com.studysyncer.backend.domain.Tag;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.UserRepository;
import com.studysyncer.backend.service.TagsService;
import com.studysyncer.backend.web.api.dto.TagCreateRequest;
import com.studysyncer.backend.web.api.dto.TagResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/tags")
public class TagApi {

    private final TagsService tagsService;
    private final UserRepository userRepository;

    public TagApi(TagsService tagsService, UserRepository userRepository) {
        this.tagsService = tagsService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @ModelAttribute TagCreateRequest req,
                                                      Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        Tag created = tagsService.createTag(user, req.getName());
        return ResponseEntity.status(201).body(Map.of(
                "ok", true,
                "tag", new TagResponse(created.getId(), created.getName())));
    }

    @PutMapping("/{id}")
    public Map<String, Object> rename(@PathVariable Long id,
                                      @Valid @ModelAttribute TagCreateRequest req,
                                      Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        Tag renamed = tagsService.renameTag(user, id, req.getName());
        return Map.of(
                "ok", true,
                "tag", new TagResponse(renamed.getId(), renamed.getName()));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        tagsService.deleteTag(user, id);
        return Map.of("ok", true);
    }
}
