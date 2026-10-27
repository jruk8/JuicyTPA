package com.jruk8.juicytpa.api;

import com.jruk8.juicytpa.message.MessagesConfig;

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
    @DisplayName("should map every mode to its configured display name")
    void shouldMapModes() {
        var names = new MessagesConfig().getTpa();
        names.setModeNormal("Custom Normal");
        names.setModeBlockAll("Custom Block All");
        names.setModeAcceptAll("Custom Accept All");

        assertEquals("Custom Normal", names.modeName(TpaMode.NORMAL));
        assertEquals("Custom Block All", names.modeName(TpaMode.BLOCK_ALL));
        assertEquals("Custom Accept All", names.modeName(TpaMode.ACCEPT_ALL));
    }
}
