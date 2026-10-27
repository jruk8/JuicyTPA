package com.jruk8.juicytpa.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginLinksTest {

    @Test
    @DisplayName("should expose all four link buttons")
    void shouldExposeLinks() {
        Map<String, String> links = PluginLinks.buttons(new MessagesConfig());

        assertEquals(4, links.size());
        assertTrue(links.containsKey("{modrinth}"));
        assertTrue(links.containsKey("{github}"));
        assertTrue(links.containsKey("{discord}"));
        assertTrue(links.containsKey("{donate}"));
    }

    @Test
    @DisplayName("should resolve each button url from code constants")
    void shouldResolveUrls() {
        Map<String, String> links = PluginLinks.buttons(new MessagesConfig());

        assertTrue(links.get("{modrinth}").contains(PluginLinks.MODRINTH_URL));
        assertTrue(links.get("{github}").contains(PluginLinks.GITHUB_URL));
        assertTrue(links.get("{discord}").contains(PluginLinks.DISCORD_URL));
        assertTrue(links.get("{donate}").contains(PluginLinks.DONATE_URL));
    }
}
