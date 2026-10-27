package com.jruk8.juicytpa.tpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TpaListFormatterTest {

    private static String join(List<String> names) {
        return TpaListFormatter.join(names, "{hl},", "{hl}and", "{plain}, and {hl}{count} more.");
    }

    @Test
    @DisplayName("should render nothing for no names")
    void shouldRenderEmpty() {
        assertEquals("", join(List.of()));
    }

    @Test
    @DisplayName("should render a lone name bare")
    void shouldRenderSingle() {
        assertEquals("Alex", join(List.of("Alex")));
    }

    @Test
    @DisplayName("should join two names with and")
    void shouldJoinPair() {
        assertEquals("Alex {hl}and Steve", join(List.of("Alex", "Steve")));
    }

    @Test
    @DisplayName("should use the Oxford comma past two names")
    void shouldJoinOxford() {
        assertEquals("Alex{hl}, Steve{hl}, {hl}and Max", join(List.of("Alex", "Steve", "Max")));
    }

    @Test
    @DisplayName("should show all ten names without truncating")
    void shouldShowTen() {
        List<String> names = new ArrayList<>();
        for (int index = 1; index <= 10; index++) {
            names.add("P" + index);
        }

        assertEquals(
                "P1{hl}, P2{hl}, P3{hl}, P4{hl}, P5{hl}, P6{hl}, P7{hl}, P8{hl}, P9{hl}, {hl}and P10",
                join(names));
    }

    @Test
    @DisplayName("should truncate past ten names with a more tail")
    void shouldTruncatePastTen() {
        List<String> names = new ArrayList<>();
        for (int index = 1; index <= 12; index++) {
            names.add("P" + index);
        }

        assertEquals(
                "P1{hl}, P2{hl}, P3{hl}, P4{hl}, P5{hl}, P6{hl}, P7{hl}, P8{hl}, P9{hl}, P10"
                        + "{plain}, and {hl}2 more.",
                join(names));
    }
}
