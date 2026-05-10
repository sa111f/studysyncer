package com.studysyncer.backend.web.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class Inputs {

    private Inputs() {}

    /** Trim and convert blanks to null. */
    public static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Parse an ISO local-datetime string from {@code <input type="datetime-local">} into an Instant in the system zone. */
    public static Instant parseLocalDateTime(String local) {
        if (local == null || local.isBlank()) return null;
        return LocalDateTime.parse(local).atZone(ZoneId.systemDefault()).toInstant();
    }
}
