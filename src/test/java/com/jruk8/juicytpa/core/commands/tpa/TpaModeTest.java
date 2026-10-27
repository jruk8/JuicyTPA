package com.jruk8.juicytpa.core.commands.tpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TpaModeTest {

    @Test
    @DisplayName("should cycle normal to block-all to accept-all and back")
    void shouldCycle() {
        assertEquals(TpaMode.BLOCK_ALL, TpaMode.NORMAL.next());
        assertEquals(TpaMode.ACCEPT_ALL, TpaMode.BLOCK_ALL.next());
        assertEquals(TpaMode.NORMAL, TpaMode.ACCEPT_ALL.next());
    }

    @Test
    @DisplayName("should parse full names and shorthands case-insensitively")
    void shouldParse() {
        assertEquals(TpaMode.NORMAL, TpaMode.parse("normal").orElseThrow());
        assertEquals(TpaMode.BLOCK_ALL, TpaMode.parse("BLOCK_ALL").orElseThrow());
        assertEquals(TpaMode.BLOCK_ALL, TpaMode.parse("block").orElseThrow());
        assertEquals(TpaMode.ACCEPT_ALL, TpaMode.parse("Accept_All").orElseThrow());
        assertEquals(TpaMode.ACCEPT_ALL, TpaMode.parse("accept").orElseThrow());
        assertTrue(TpaMode.parse("nope").isEmpty());
    }

    @Test
    @DisplayName("should expose friendly display names")
    void shouldDisplay() {
        assertEquals("Normal", TpaMode.NORMAL.display());
        assertEquals("Block all", TpaMode.BLOCK_ALL.display());
        assertEquals("Accept all", TpaMode.ACCEPT_ALL.display());
    }
}
