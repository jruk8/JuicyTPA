package com.jruk8.jtemplate.core.commands.selectors;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.jruk8.jtemplate.core.commands.TemplatePermissions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

class TargetSelectorSuggestTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private CommandSourceStack source(Player sender) {
        CommandSourceStack source = mock(CommandSourceStack.class);
        lenient().when(source.getSender()).thenReturn(sender);
        return source;
    }

    private Player player(String name, String... nodes) {
        Player player = mock(Player.class);
        lenient().when(player.getName()).thenReturn(name);
        for (String node : nodes) {
            lenient().when(player.hasPermission(node)).thenReturn(true);
        }
        return player;
    }

    @Test
    @DisplayName("should suggest selectors first with the other permission")
    void shouldSuggestSelectorsWithOther() {
        var source = source(player("Steve", TemplatePermissions.SELF, TemplatePermissions.OTHER));

        assertEquals(List.of("@p", "@a", "@r", "@s"), TargetSelector.suggestTargets(source, ""));
    }

    @Test
    @DisplayName("should suggest only self entries without the other permission")
    void shouldSuggestSelfOnlyWithoutOther() {
        var source = source(player("Steve", TemplatePermissions.SELF));

        assertEquals(List.of("@s", "Steve"), TargetSelector.suggestTargets(source, ""));
    }

    @Test
    @DisplayName("should remove self entries without the self permission")
    void shouldRemoveSelfEntriesWithoutSelf() {
        var source = source(player("Steve", TemplatePermissions.OTHER));

        assertEquals(List.of("@p", "@a", "@r"), TargetSelector.suggestTargets(source, ""));
    }

    @Test
    @DisplayName("should suggest nothing without either permission")
    void shouldSuggestNothingWithoutScope() {
        var source = source(player("Steve"));

        assertEquals(List.of(), TargetSelector.suggestTargets(source, ""));
    }
}
