package com.jruk8.juicytpa.update;

import com.jruk8.juicytpa.command.Permissions;
import com.jruk8.juicytpa.message.MessageBundles;
import com.jruk8.juicytpa.message.Messenger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Sends update notices to players holding {@code juicytpa.reload}.
 */
public final class UpdateCheckNotifier {

    private final Messenger messenger;
    private final MessageBundles bundles;

    public UpdateCheckNotifier(Messenger messenger, MessageBundles bundles) {
        this.messenger = messenger;
        this.bundles = bundles;
    }

    /**
     * Sends an update notice to every permitted online player.
     *
     * @param update the found update
     */
    public void notifyAdmins(UpdateAvailable update) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            notifyPlayer(player, update);
        }
    }

    /**
     * Sends an update notice to one player when permitted.
     *
     * @param player the player to notify
     * @param update the found update
     */
    public void notifyPlayer(Player player, UpdateAvailable update) {
        if (player == null || update == null || !player.hasPermission(Permissions.RELOAD)) {
            return;
        }
        var tags = Map.of(
                "latest", update.latest(),
                "current", update.current(),
                "url", update.url(),
                "modrinth-url", update.modrinthUrl());
        messenger.send(player, bundles.configFor(player).getCore().getUpdateAvailable(), tags);
    }
}
