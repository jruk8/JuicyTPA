package com.jruk8.juicytpa.command.admin.reload;

import com.jruk8.juicytpa.core.Reloader;
import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.config.PluginConfig;
import com.jruk8.juicytpa.message.MessagesConfig;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReloadCommandTest {

    @Mock
    private Reloader reloader;

    @Mock
    private CommandContext ctx;

    @Mock
    private CommandSourceStack source;

    @Captor
    private ArgumentCaptor<Function<MessagesConfig, String>> selectorCaptor;

    private ReloadCommand command(PluginConfig config) {
        return new ReloadCommand(reloader, ctx, config);
    }

    @Test
    @DisplayName("should report success without a restart notice when toggles are unchanged")
    void shouldReportSuccess() {
        when(reloader.reloadAll()).thenReturn(true);

        command(new PluginConfig()).onReload(source);

        verify(ctx).sendReload(eq(source), any(), anyMap());
        verify(ctx, never()).error(eq(source), any(), anyMap());
        verify(ctx, never()).sendMessage(eq(source), any(), anyMap());
    }

    @Test
    @DisplayName("should report failure when any reloadable throws")
    void shouldReportFailure() {
        when(reloader.reloadAll()).thenReturn(false);

        command(new PluginConfig()).onReload(source);

        verify(ctx).error(eq(source), any(), anyMap());
        verify(ctx, never()).sendReload(eq(source), any(), anyMap());
    }

    @Test
    @DisplayName("should ask for a restart when labels changed during reload")
    void shouldAskForRestartOnLabelChange() {
        var config = new PluginConfig();
        when(reloader.reloadAll()).thenAnswer(ignored -> {
            config.setAliases(List.of("jt", "template"));
            return true;
        });

        command(config).onReload(source);

        verify(ctx).sendMessage(eq(source), selectorCaptor.capture(), anyMap());
        var messages = new MessagesConfig();
        var expected = messages.getCore().getReloadRestartRequired();
        assertEquals(expected, selectorCaptor.getValue().apply(messages));
    }
}
