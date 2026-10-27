package com.jruk8.juicytpa.core.hooks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * LuckPerms soft boundary for per-player meta lookups. Only
 * {@link #meta} is ever called, and it touches {@link Reader} solely
 * inside the plugin-present branch, so this class loads safely without
 * LuckPerms installed.
 */
public final class LuckPermsMeta {

    private LuckPermsMeta() {
    }

    /**
     * Reads one meta value for an online player.
     *
     * @param player the player
     * @param key the meta key
     * @return the raw meta value, or null when LuckPerms is absent,
     * the lookup fails or the key is unset. Never throws.
     */
    public static String meta(Player player, String key) {
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            return null;
        }
        try {
            return Reader.read(player, key);
        } catch (Exception broken) {
            Bukkit.getLogger().warning("LuckPerms meta lookup failed: " + broken.getMessage());
            return null;
        }
    }

    /**
     * Isolated reader: LuckPerms types only load when the plugin is
     * present, mirroring the sibling JManhunt hook.
     */
    private static final class Reader {

        private Reader() {
        }

        private static String read(Player player, String key) {
            LuckPerms api = LuckPermsProvider.get();
            User user = api.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                return null;
            }
            return user.getCachedData().getMetaData().getMetaValue(key);
        }
    }
}
