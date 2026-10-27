package com.jruk8.juicytpa.message;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles the parsing and dispatching of MiniMessage-formatted text to command senders.
 * <p>
 * Acts as a bridge between raw message strings, {bracket} placeholders,
 * and Bukkit's {@link CommandSender} interface.
 */
public final class Messenger {

    private final MessageParser messageParser;
    private final MessageBundles bundles;

    /**
     * Constructs a new {@code Messenger} with the given parser.
     *
     * @param messageParser the parser used to process raw strings into components
     * @param bundles the per-language bundles link buttons resolve from
     */
    public Messenger(MessageParser messageParser, MessageBundles bundles) {
        this.messageParser = messageParser;
        this.bundles = bundles;
    }

    /**
     * Parses a raw string with {bracket} placeholders, then sends it to the target recipient.
     *
     * @param sender             the recipient of the message; if null, operation is ignored
     * @param rawMessage         the unparsed message string; if null or empty, operation is ignored
     * @param stringPlaceholders key-value pairs for {bracket} text replacements
     */
    public void send(CommandSender sender,
                     String rawMessage,
                     Map<String, String> stringPlaceholders) {
        if (sender == null || rawMessage == null || rawMessage.isEmpty()) {
            return;
        }

        Component text = messageParser.parse(rawMessage, withLinks(sender, stringPlaceholders));
        sender.sendMessage(text);
    }

    /**
     * Parses a raw string with no placeholders and sends it to the target recipient.
     *
     * @param sender     the recipient of the message
     * @param rawMessage the unparsed message string
     */
    public void send(CommandSender sender, String rawMessage) {
        send(sender, rawMessage, Map.of());
    }

    /**
     * Parses a raw string with {bracket} placeholders, then shows it on the
     * player's actionbar. Non-player senders are silently ignored.
     *
     * @param sender             the recipient of the actionbar
     * @param rawMessage         the unparsed message string; if null or empty, operation is ignored
     * @param stringPlaceholders key-value pairs for {bracket} text replacements
     */
    public void sendActionBar(CommandSender sender,
                              String rawMessage,
                              Map<String, String> stringPlaceholders) {
        if (!(sender instanceof Player player) || rawMessage == null || rawMessage.isEmpty()) {
            return;
        }

        Component text = messageParser.parse(rawMessage, withLinks(sender, stringPlaceholders));
        player.sendActionBar(text);
    }

    private Map<String, String> withLinks(CommandSender sender, Map<String, String> tags) {
        Map<String, String> merged = new HashMap<>(PluginLinks.buttons(bundles.configFor(sender)));
        if (tags != null) {
            merged.putAll(tags);
        }
        return merged;
    }
}
