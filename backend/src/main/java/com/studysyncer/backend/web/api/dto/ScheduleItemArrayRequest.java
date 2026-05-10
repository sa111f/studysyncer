package com.studysyncer.backend.web.api.dto;

import com.studysyncer.backend.domain.ScheduleItemType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Parallel-array binding for the schedule sub-form. Spring binds form fields named
 * {@code scheduleIds, scheduleTitles, scheduleLocations, scheduleDays,
 * scheduleStartTimes, scheduleDurations, scheduleTypes} (each repeated per row)
 * into the corresponding {@code List<String>}.
 */
@Getter
@Setter
@NoArgsConstructor
public class ScheduleItemArrayRequest {

    private List<String> scheduleIds = new ArrayList<>();
    private List<String> scheduleTitles = new ArrayList<>();
    private List<String> scheduleLocations = new ArrayList<>();
    private List<String> scheduleDays = new ArrayList<>();
    private List<String> scheduleStartTimes = new ArrayList<>();
    private List<String> scheduleDurations = new ArrayList<>();
    private List<String> scheduleTypes = new ArrayList<>();

    public List<ScheduleItemRequest> toList() {
        int n = scheduleTitles.size();
        List<ScheduleItemRequest> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ScheduleItemRequest r = new ScheduleItemRequest();
            r.setId(parseLong(get(scheduleIds, i)));
            r.setTitle(get(scheduleTitles, i));
            r.setLocation(get(scheduleLocations, i));
            r.setDayOfWeek(parseShort(get(scheduleDays, i)));
            r.setStartTimeLocal(get(scheduleStartTimes, i));
            r.setDurationMinutes(parseInt(get(scheduleDurations, i)));
            String typeStr = get(scheduleTypes, i);
            r.setItemType(typeStr != null && !typeStr.isBlank()
                    ? ScheduleItemType.valueOf(typeStr)
                    : ScheduleItemType.LECTURE);
            out.add(r);
        }
        return out;
    }

    private static String get(List<String> list, int i) {
        return list != null && i < list.size() ? list.get(i) : null;
    }

    private static Long parseLong(String s) {
        return s == null || s.isBlank() ? null : Long.parseLong(s);
    }

    private static Short parseShort(String s) {
        return s == null || s.isBlank() ? null : Short.parseShort(s);
    }

    private static Integer parseInt(String s) {
        return s == null || s.isBlank() ? null : Integer.parseInt(s);
    }
}
