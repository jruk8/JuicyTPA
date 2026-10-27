package com.jruk8.juicytpa.message;

import com.jruk8.juicytpa.core.Reloadable;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The per-language message bundles. English ({@code messages.yml})
 * is always the fallback: every other language loads its
 * {@code lang/<code>.yml} over a fresh English schema, so missing
 * files and missing keys both read English. Files seed from the
 * bundled translations on first use and stay admin-editable.
 */
public final class MessageBundles implements Reloadable {

    private final MessagesConfig english;
    private final Function<UUID, String> pinLookup;
    private final File langDir;
    private final Logger logger;
    private final Map<String, MessagesConfig> cache = new ConcurrentHashMap<>();

    /**
     * English-only bundles for tests: every sender reads the given config.
     *
     * @param english the config every sender reads
     */
    public MessageBundles(MessagesConfig english) {
        this(english, uuid -> null, null, Logger.getLogger(MessageBundles.class.getName()));
    }

    /**
     * @param english the English config ({@code messages.yml}), always the fallback
     * @param pinLookup the pinned code per player, or null to follow the client locale
     * @param langDir the data-folder {@code lang} directory holding the translations
     * @param logger the plugin logger
     */
    public MessageBundles(
            MessagesConfig english,
            Function<UUID, String> pinLookup,
            File langDir,
            Logger logger
    ) {
        this.english = english;
        this.pinLookup = pinLookup;
        this.langDir = langDir;
        this.logger = logger;
    }

    /**
     * @return the English bundle, the fallback for every sender
     */
    public MessagesConfig english() {
        return english;
    }

    /**
     * Resolves the bundle one sender reads: their pinned language,
     * else their live client locale, else English. Non-players and
     * anything unknown read English.
     *
     * @param sender the recipient
     * @return the recipient's bundle, never null
     */
    public MessagesConfig configFor(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return english;
        }
        String pin = pinLookup.apply(player.getUniqueId());
        if (pin != null) {
            return configForCode(pin);
        }
        Locale locale = player.locale();
        return configForCode(locale == null ? TpaLanguages.ENGLISH : TpaLanguages.normalize(locale.toLanguageTag()));
    }

    /**
     * @param code the language code
     * @return the cached bundle, or English on anything unknown
     */
    public MessagesConfig configForCode(String code) {
        if (!TpaLanguages.isSupported(code) || TpaLanguages.ENGLISH.equals(code) || langDir == null) {
            return english;
        }
        return cache.computeIfAbsent(code, this::load);
    }

    @Override
    public void reload() {
        cache.clear();
    }

    private MessagesConfig load(String code) {
        File file = new File(langDir, code + ".yml");
        seed(code, file);
        try {
            return ConfigManager.create(MessagesConfig.class, it -> {
                it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
                it.withBindFile(file);
                it.load();
            });
        } catch (Exception e) {
            logger.log(Level.WARNING, "Falling back to English; could not load lang/" + code + ".yml", e);
            return english;
        }
    }

    private void seed(String code, File file) {
        if (file.exists()) {
            return;
        }
        try {
            Files.createDirectories(file.getParentFile().toPath());
            try (InputStream bundled = resource("lang/" + code + ".yml")) {
                if (bundled == null) {
                    Files.createFile(file.toPath());
                } else {
                    Files.copy(bundled, file.toPath());
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not seed lang/" + code + ".yml; English fallback applies", e);
        }
    }

    private static InputStream resource(String path) {
        return MessageBundles.class.getClassLoader().getResourceAsStream(path);
    }
}
