package com.studysyncer.backend.web.advice;

import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.Exam;
import com.studysyncer.backend.domain.ExamStatus;
import com.studysyncer.backend.domain.Tag;
import com.studysyncer.backend.domain.TaskStatus;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.CourseRepository;
import com.studysyncer.backend.repository.ExamRepository;
import com.studysyncer.backend.repository.TagRepository;
import com.studysyncer.backend.repository.TaskRepository;
import com.studysyncer.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ControllerAdvice
public class SidebarAdvice {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final TagRepository tagRepository;
    private final TaskRepository taskRepository;
    private final ExamRepository examRepository;

    public SidebarAdvice(UserRepository userRepository,
                         CourseRepository courseRepository,
                         TagRepository tagRepository,
                         TaskRepository taskRepository,
                         ExamRepository examRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.tagRepository = tagRepository;
        this.taskRepository = taskRepository;
        this.examRepository = examRepository;
    }

    @ModelAttribute
    public void populateSidebar(HttpServletRequest request, Model model) {
        String path = request.getRequestURI();
        if (!isSidebarPath(path)) {
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return;
        }

        Optional<User> userOpt = userRepository.findByEmail(auth.getName());
        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.get();

        List<Course> courses = courseRepository.findByUserOrderByDisplayOrderAsc(user);
        List<Tag> tags = tagRepository.findByUserOrderByNameAsc(user);

        long pendingTaskCount = taskRepository.countByUserAndStatus(user, TaskStatus.PENDING);

        ZoneId tz = ZoneId.systemDefault();
        Long examDays = examRepository
                .findTop2ByUserAndStatusOrderByStartsAtAsc(user, ExamStatus.UPCOMING)
                .stream()
                .findFirst()
                .map(Exam::getStartsAt)
                .map(starts -> ChronoUnit.DAYS.between(LocalDate.now(tz), starts.atZone(tz).toLocalDate()))
                .filter(d -> d >= 0)
                .orElse(null);

        Map<Long, Long> courseCounts = new LinkedHashMap<>();
        for (Course course : courses) {
            long c = taskRepository.countByUserAndCourseAndStatus(user, course, TaskStatus.PENDING);
            courseCounts.put(course.getId(), c);
        }

        model.addAttribute("sidebarUser", user);
        model.addAttribute("sidebarCourses", courses);
        model.addAttribute("sidebarTags", tags);
        model.addAttribute("currentNav", currentNavFor(path));
        model.addAttribute("currentCourseSlug", currentCourseSlugFor(path));
        model.addAttribute("sidebarTaskCount", pendingTaskCount);
        model.addAttribute("sidebarExamDays", examDays);
        model.addAttribute("sidebarCourseCounts", courseCounts);
    }

    private static String currentCourseSlugFor(String path) {
        if (path == null) return null;
        String prefix = "/spaces/";
        if (!path.startsWith(prefix)) return null;
        String rest = path.substring(prefix.length());
        int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }

    private static boolean isSidebarPath(String path) {
        if (path == null) return false;
        if (path.equals("/login") || path.equals("/signup") || path.equals("/error")) return false;
        if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/fonts/")) return false;
        return true;
    }

    private static String currentNavFor(String path) {
        if (path.equals("/")) return "today";
        if (path.equals("/tasks") || path.startsWith("/tasks/")) return "tasks";
        if (path.equals("/exams") || path.startsWith("/exams/")) return "exams";
        if (path.equals("/tracker") || path.startsWith("/tracker/")) return "tracker";
        if (path.equals("/focus") || path.startsWith("/focus/")) return "focus";
        return null;
    }
}
