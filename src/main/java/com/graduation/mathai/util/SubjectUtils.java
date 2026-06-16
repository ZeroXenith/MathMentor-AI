package com.graduation.mathai.util;

import org.springframework.util.StringUtils;

/**
 * Subject normalization utility — shared across services to avoid duplicate logic.
 */
public final class SubjectUtils {

    public static final String MATH = "math";
    public static final String ENGLISH = "english";

    private SubjectUtils() {
        // utility class
    }

    /**
     * Normalize a subject string to "math" or "english".
     * Defaults to "math" when input is blank or unrecognized.
     */
    public static String normalize(String subject) {
        if (!StringUtils.hasText(subject)) {
            return MATH;
        }
        String value = subject.trim().toLowerCase();
        return ENGLISH.equals(value) ? ENGLISH : MATH;
    }
}
