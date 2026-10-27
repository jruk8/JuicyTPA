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
    @DisplayName("should expose friendly display names")
    void shouldDisplay() {
        var names = new MessagesConfig().getTpa();
        assertEquals("Normal", names.modeName(TpaMode.NORMAL));
        assertEquals("<#f5c542>Block All", names.modeName(TpaMode.BLOCK_ALL));
        assertEquals("<#f5535e>Accept All", names.modeName(TpaMode.ACCEPT_ALL));
    }

    @Test
    @DisplayName("should read renamed display names")
    void shouldReadRenamed() {
        var names = new MessagesConfig().getTpa();
        names.setModeBlockAll("Nope");

        assertEquals("Nope", names.modeName(TpaMode.BLOCK_ALL));
    }
}
