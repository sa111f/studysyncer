package com.studysyncer.backend.web.api;

import com.studysyncer.backend.domain.Exam;
import com.studysyncer.backend.domain.ExamTopic;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.ExamTopicRepository;
import com.studysyncer.backend.repository.UserRepository;
import com.studysyncer.backend.service.ExamsService;
import com.studysyncer.backend.web.api.dto.ExamCreateRequest;
import com.studysyncer.backend.web.api.dto.ExamResponse;
import com.studysyncer.backend.web.api.dto.TopicResponse;
import com.studysyncer.backend.web.api.dto.TopicUpdateRequest;
import com.studysyncer.backend.web.view.Formatters;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exams")
public class ExamApi {

    private static final int RING_CIRCUMFERENCE = 264;

    private final ExamsService examsService;
    private final ExamTopicRepository topicRepository;
    private final UserRepository userRepository;
    private final Formatters formatters;

    public ExamApi(ExamsService examsService,
                   ExamTopicRepository topicRepository,
                   UserRepository userRepository,
                   Formatters formatters) {
        this.examsService = examsService;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.formatters = formatters;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @ModelAttribute ExamCreateRequest req,
                                                      Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        Exam created = examsService.createExam(user, req);
        List<ExamTopic> topics = topicRepository.findByExamOrderByDisplayOrderAsc(created);
        return ResponseEntity.status(201).body(Map.of(
                "ok", true,
                "exam", ExamResponse.from(created, topics, formatters)));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id,
                                      @Valid @ModelAttribute ExamCreateRequest req,
                                      Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        Exam updated = examsService.updateExam(user, id, req);
        List<ExamTopic> topics = topicRepository.findByExamOrderByDisplayOrderAsc(updated);
        return Map.of(
                "ok", true,
                "exam", ExamResponse.from(updated, topics, formatters));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        examsService.deleteExam(user, id);
        return Map.of("ok", true);
    }

    @PostMapping("/{examId}/topics")
    public ResponseEntity<Map<String, Object>> addTopic(@PathVariable Long examId,
                                                        @RequestParam String name,
                                                        Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        ExamTopic t = examsService.addTopic(user, examId, name);
        return ResponseEntity.status(201).body(Map.of(
                "ok", true,
                "topic", TopicResponse.from(t)));
    }

    @PutMapping("/topics/{topicId}")
    public Map<String, Object> updateTopic(@PathVariable Long topicId,
                                           @Valid @ModelAttribute TopicUpdateRequest req,
                                           Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        ExamTopic t = examsService.updateTopic(user, topicId, req);
        Exam ex = t.getExam();
        List<ExamTopic> all = topicRepository.findByExamOrderByDisplayOrderAsc(ex);
        int score = all.isEmpty() ? 0
                : (int) Math.round(100.0 * all.stream()
                .mapToDouble(x -> switch (x.getStatus()) {
                    case DONE -> 1.0;
                    case NEUTRAL -> 0.5;
                    case WEAK -> 0.0;
                }).sum() / all.size());
        return Map.of(
                "ok", true,
                "topic", TopicResponse.from(t),
                "examId", ex.getId(),
                "readinessPercent", score,
                "dashOffset", RING_CIRCUMFERENCE - (RING_CIRCUMFERENCE * score / 100));
    }

    @DeleteMapping("/topics/{topicId}")
    public Map<String, Object> deleteTopic(@PathVariable Long topicId, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        examsService.deleteTopic(user, topicId);
        return Map.of("ok", true);
    }
}
