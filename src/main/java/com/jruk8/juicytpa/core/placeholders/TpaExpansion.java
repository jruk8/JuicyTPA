package com.jruk8.juicytpa.core.placeholders;

import com.jruk8.juicytpa.core.commands.tpa.TpaModeStore;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * The JuicyTPA PlaceholderAPI expansion.
 * <p>
 * Identifier {@code jtpa}, with self, other-player and global variants of
 * the {@code sent} and {@code received} counters, plus self and
 * other-player variants of the {@code mode} name. Unknown players read as
 * {@code 0}; the self variants need a player context.
 */
public final class TpaExpansion extends PlaceholderExpansion {

    public static final String IDENTIFIER = "jtpa";
    public static final String SENT = "sent";
    public static final String RECEIVED = "received";
    public static final String MODE = "mode";
    public static final String GLOBAL = "global";

    private final TpaUsageTracker tracker;
    private final TpaModeStore modes;
    private final JavaPlugin plugin;
    private final Function<String, UUID> nameResolver;

    public TpaExpansion(
            TpaUsageTracker tracker,
            TpaModeStore modes,
            JavaPlugin plugin,
            Function<String, UUID> nameResolver
    ) {
        this.tracker = tracker;
        this.modes = modes;
        this.plugin = plugin;
        this.nameResolver = nameResolver;
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getAuthor() {
        List<String> authors = this.plugin.getPluginMeta().getAuthors();
        return authors.isEmpty() ? this.plugin.getName() : authors.getFirst();
    }

    @Override
    public String getVersion() {
        return this.plugin.getPluginMeta().getVersion();
    }

    @Override
    public String getRequiredPlugin() {
        return this.plugin.getName();
    }

    @Override
    public List<String> getPlaceholders() {
        return List.of(
                "%" + IDENTIFIER + "_" + SENT + "%",
                "%" + IDENTIFIER + "_" + SENT + "_<player>%",
                "%" + IDENTIFIER + "_" + SENT + "_" + GLOBAL + "%",
                "%" + IDENTIFIER + "_" + RECEIVED + "%",
                "%" + IDENTIFIER + "_" + RECEIVED + "_<player>%",
                "%" + IDENTIFIER + "_" + RECEIVED + "_" + GLOBAL + "%",
                "%" + IDENTIFIER + "_" + MODE + "%",
                "%" + IDENTIFIER + "_" + MODE + "_<player>%"
        );
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String sent = counterFor(player, SENT, params, this.tracker::playerSent, this.tracker.globalSent());
        if (sent != null) {
            return sent;
        }
        String received = counterFor(
                player, RECEIVED, params, this.tracker::playerReceived, this.tracker.globalReceived());
        if (received != null) {
            return received;
        }
        if (MODE.equals(params)) {
            return player == null ? null : this.modes.mode(player.getUniqueId()).display();
        }
        if (params.startsWith(MODE + "_")) {
            String name = params.substring(MODE.length() + 1);
            return name.isEmpty() ? null : this.modes.mode(this.nameResolver.apply(name)).display();
        }
        return null;
    }

    private String counterFor(
            OfflinePlayer viewer, String key, String params, ToIntFunction<UUID> read, int global) {
        if (key.equals(params)) {
            return viewer == null ? null : String.valueOf(read.applyAsInt(viewer.getUniqueId()));
        }
        if ((key + "_" + GLOBAL).equals(params)) {
            return String.valueOf(global);
        }
        if (params.startsWith(key + "_")) {
            String name = params.substring(key.length() + 1);
            return name.isEmpty() ? null : String.valueOf(read.applyAsInt(this.nameResolver.apply(name)));
        }
        return null;
    }
}
