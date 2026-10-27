package com.jruk8.juicytpa.message;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangResourcesTest {

    private static ServerMock server;

    private final MessagesConfig english = new MessagesConfig();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @BeforeAll
    static void startServer() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("should load every shipped language")
    void shouldLoadEveryLanguage(@TempDir Path dir) {
        var bundles = new MessageBundles(
                english, uuid -> null, dir.toFile(), Logger.getLogger("lang-resources-test"), () -> "auto");

        for (String code : TpaLanguages.SUPPORTED) {
            if (TpaLanguages.ENGLISH.equals(code)) {
                continue;
            }
            assertNotSame(english, bundles.configForCode(code), "lang/" + code + ".yml");
            assertTrue(dir.resolve(code + ".yml").toFile().exists(), "lang/" + code + ".yml");
        }
    }

    @Test
    @DisplayName("should keep every translated value non-blank and MiniMessage-clean")
    void shouldKeepValuesClean(@TempDir Path dir) {
        var bundles = new MessageBundles(
                english, uuid -> null, dir.toFile(), Logger.getLogger("lang-resources-test"), () -> "auto");

        for (String code : TpaLanguages.SUPPORTED) {
            MessagesConfig bundle = bundles.configForCode(code);
            for (String value : values(bundle)) {
                assertFalse(value.isBlank(), "blank value in lang/" + code + ".yml");
                miniMessage.deserialize(value.replace("\\n", "\n"));
            }
        }
    }

    private static List<String> values(MessagesConfig bundle) {
        List<String> values = new ArrayList<>();
        values.add(bundle.getPrefix());
        for (Object section : List.of(bundle.getCore(), bundle.getTpa(), bundle.getHelp())) {
            for (Method method : section.getClass().getMethods()) {
                if (!method.getName().startsWith("get")
                        || method.getParameterCount() != 0
                        || method.getReturnType() != String.class
                        || method.getDeclaringClass() == Object.class) {
                    continue;
                }
                try {
                    values.add((String) method.invoke(section));
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot read " + method.getName(), e);
                }
            }
        }
        return values;
    }
}
