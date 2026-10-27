package com.jruk8.juicytpa.core.commands.tpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurationFormatTest {

    @Test
    @DisplayName("should render seconds alone")
    void shouldRenderSeconds() {
        assertEquals("48s", DurationFormat.format(48));
    }

    @Test
    @DisplayName("should omit zero units")
    void shouldOmitZeroUnits() {
        assertEquals("19m 25s", DurationFormat.format(19 * 60 + 25));
        assertEquals("1h 2m 3s", DurationFormat.format(3_600 + 2 * 60 + 3));
        assertEquals("1d 1h", DurationFormat.format(86_400 + 3_600));
        assertEquals("2m", DurationFormat.format(120));
    }

    @Test
    @DisplayName("should clamp negatives to zero")
    void shouldClampNegatives() {
        assertEquals("0s", DurationFormat.format(0));
        assertEquals("0s", DurationFormat.format(-5));
    }
}
