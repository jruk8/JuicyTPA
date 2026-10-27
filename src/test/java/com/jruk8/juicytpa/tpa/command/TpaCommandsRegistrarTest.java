package com.jruk8.juicytpa.tpa.command;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.config.PluginConfig;
import com.jruk8.juicytpa.core.Reloader;
import com.jruk8.juicytpa.message.MessageBundles;
import com.jruk8.juicytpa.message.MessageParser;
import com.jruk8.juicytpa.message.MessagesConfig;
import com.jruk8.juicytpa.message.Messenger;
import com.jruk8.juicytpa.sound.SoundPlayer;
import com.jruk8.juicytpa.sound.SoundsConfig;
import com.jruk8.juicytpa.tpa.PlayerLanguage;
import com.jruk8.juicytpa.tpa.TpaService;
import com.jruk8.juicytpa.update.UpdateCheckService;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TpaCommandsRegistrarTest {

    private static ServerMock server;

    @BeforeAll
    static void startServer() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
    }

    private TpaCommandsRegistrar registrar(PluginConfig pluginConfig) {
        var bundles = new MessageBundles(new MessagesConfig());
        var ctx = new CommandContext(
                new MessagesConfig(),
                new SoundsConfig(),
                new Messenger(mock(MessageParser.class), bundles),
                new SoundPlayer(Logger.getLogger("registrar-test")),
                bundles);
        return new TpaCommandsRegistrar(
                Commands.literal("hub"),
                ctx,
                mock(TpaService.class),
                mock(PlayerLanguage.class),
                pluginConfig,
                mock(Plugin.class),
                Logger.getLogger("registrar-test"),
                mock(PluginMeta.class),
                mock(UpdateCheckService.class),
                mock(Reloader.class));
    }

    @Test
    @DisplayName("should mirror every root and alias under jtpa:")
    void shouldMirrorNamespacedTwins() {
        var pluginConfig = new PluginConfig();
        pluginConfig.getCommands().getTpa().setAliases(List.of("tpask", "tpav"));
        pluginConfig.getCommands().getTpall().setEnabled(false);
        var commands = mock(Commands.class);

        registrar(pluginConfig).registerStandalone(commands, List.of());

        var nodeCaptor = ArgumentCaptor.forClass(LiteralCommandNode.class);
        var aliasCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(commands, atLeastOnce()).register(nodeCaptor.capture(), anyString(), aliasCaptor.capture());

        List<LiteralCommandNode> nodes = nodeCaptor.getAllValues();
        Set<String> literals = nodes.stream()
                .map(node -> node.getLiteral().toString())
                .collect(Collectors.toSet());
        assertTrue(literals.contains("tpa"));
        assertTrue(literals.contains("jtpa:tpask"));
        assertFalse(literals.contains("tpall"));
        assertFalse(literals.contains("jtpa:tpall"));
        assertTrue(literals.contains("tpahelp"));
        assertTrue(literals.contains("tpaversion"));
        assertTrue(literals.contains("tpareload"));
        assertEquals(List.of("tpask"), aliasesOf(nodes, aliasCaptor.getAllValues(), "tpa"));
        assertEquals(List.of("tpav"), aliasesOf(nodes, aliasCaptor.getAllValues(), "tpaversion"));
        for (String literal : literals) {
            if (!literal.contains(":")) {
                assertTrue(literals.contains("jtpa:" + literal), literal);
            }
        }
        for (Object aliases : aliasCaptor.getAllValues()) {
            for (Object alias : (Collection<?>) aliases) {
                assertTrue(literals.contains("jtpa:" + alias), String.valueOf(alias));
            }
        }
    }

    private static List<?> aliasesOf(
            List<LiteralCommandNode> nodes, List<Collection> aliasLists, String literal) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getLiteral().toString().equals(literal)) {
                return List.copyOf(aliasLists.get(i));
            }
        }
        throw new IllegalArgumentException("No such root: " + literal);
    }
}
