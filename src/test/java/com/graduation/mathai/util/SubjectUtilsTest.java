package com.graduation.mathai.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubjectUtilsTest {

    @Test
    void normalize_blank_returnsMath() {
        assertEquals("math", SubjectUtils.normalize(null));
        assertEquals("math", SubjectUtils.normalize(""));
        assertEquals("math", SubjectUtils.normalize("   "));
    }

    @Test
    void normalize_english_returnsEnglish() {
        assertEquals("english", SubjectUtils.normalize("english"));
        assertEquals("english", SubjectUtils.normalize("ENGLISH"));
        assertEquals("english", SubjectUtils.normalize("English"));
    }

    @Test
    void normalize_unknown_returnsMath() {
        assertEquals("math", SubjectUtils.normalize("physics"));
        assertEquals("math", SubjectUtils.normalize("化学"));
    }
}
