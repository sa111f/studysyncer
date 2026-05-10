package com.studysyncer.backend.web.api;

import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.ScheduleItem;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.CourseRepository;
import com.studysyncer.backend.repository.ScheduleItemRepository;
import com.studysyncer.backend.repository.UserRepository;
import com.studysyncer.backend.service.SpacesService;
import com.studysyncer.backend.web.api.dto.CourseCreateRequest;
import com.studysyncer.backend.web.api.dto.CourseResponse;
import com.studysyncer.backend.web.api.dto.ScheduleItemArrayRequest;
import com.studysyncer.backend.web.api.dto.ScheduleItemRequest;
import com.studysyncer.backend.web.view.Formatters;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/courses")
public class CourseApi {

    private final SpacesService spacesService;
    private final ScheduleItemRepository scheduleRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final Formatters formatters;

    public CourseApi(SpacesService spacesService,
                     ScheduleItemRepository scheduleRepository,
                     CourseRepository courseRepository,
                     UserRepository userRepository,
                     Formatters formatters) {
        this.spacesService = spacesService;
        this.scheduleRepository = scheduleRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.formatters = formatters;
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        Course c = courseRepository.findById(id)
                .filter(x -> x.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new NoSuchElementException("Course not found"));
        List<ScheduleItem> items = scheduleRepository.findByCourseOrderByDayOfWeekAscStartMinuteAsc(c);
        return Map.of("ok", true, "course", CourseResponse.from(c, items, formatters));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @ModelAttribute CourseCreateRequest req,
                                                      @ModelAttribute ScheduleItemArrayRequest schedule,
                                                      Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        List<ScheduleItemRequest> items = schedule.toList();
        Course created = spacesService.createCourse(user, req, items);
        List<ScheduleItem> savedItems = scheduleRepository.findByCourseOrderByDayOfWeekAscStartMinuteAsc(created);
        return ResponseEntity.status(201).body(Map.of(
                "ok", true,
                "course", CourseResponse.from(created, savedItems, formatters)));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id,
                                      @Valid @ModelAttribute CourseCreateRequest req,
                                      @ModelAttribute ScheduleItemArrayRequest schedule,
                                      Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        List<ScheduleItemRequest> items = schedule.toList();
        Course updated = spacesService.updateCourse(user, id, req, items);
        List<ScheduleItem> savedItems = scheduleRepository.findByCourseOrderByDayOfWeekAscStartMinuteAsc(updated);
        return Map.of(
                "ok", true,
                "course", CourseResponse.from(updated, savedItems, formatters));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        spacesService.deleteCourse(user, id);
        return Map.of("ok", true);
    }
}
