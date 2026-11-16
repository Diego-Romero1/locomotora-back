package com.locomotora.demo.user.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProfileJsonTest {
    @Test
    void dietaryPreferencesAreReadAsTextArray() {
        assertEquals(List.of("VEGETARIAN", "sin picante"), ProfileRepository.readStringList("[\"VEGETARIAN\", \"sin picante\", \" \"]"));
        assertEquals(List.of(), ProfileRepository.readStringList("{}"));
        assertEquals(List.of(), ProfileRepository.readStringList(null));
        assertEquals(List.of(), ProfileRepository.readStringList("no es json"));
    }

    @Test
    void writesEscapedJson() {
        assertEquals("[\"sin \\\"picante\\\"\",\"VEGAN\"]", ProfileRepository.writeStringList(List.of("sin \"picante\"", "VEGAN")));
    }
}
