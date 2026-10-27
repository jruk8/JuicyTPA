package com.jruk8.juicytpa.core.messages;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.Setter;

/**
 * Messages shown by the plugin.
 * <p>
 * Every message starts with {prefix} and uses the config.yml color palette:
 * {plain} for body text, {brand} for headers and the brand name, {hl} for
 * numbers, player names and similar values, {error} for errors, {ac} for
 * accept prompts, {rc} for reject prompts, {sc} for sender feedback and
 * {lh} for warning highlights.
 * Commands insert runtime values through {bracket} placeholders: {version},
 * {authors}, {time}, {syntax}, {description}, {latest}, {current}, {url},
 * {modrinth-url}, {permission}, {player}, {mode}, {input}, {count}, {type},
 * {modrinth}, {github}, {discord} and {donate},
 * {accept}, {reject}, {body} and {button}. Angle brackets are reserved for
 * raw MiniMessage, and \n breaks the line.
 */
@Getter
@Setter
public class MessagesConfig extends OkaeriConfig {

    @Getter
    @Setter
    @SuppressWarnings("FieldMayBeFinal")
    public static class Core extends OkaeriConfig {

        @Comment("Sent when a reload succeeds (use {time})")
        private String reloadSuccess = "\\n{prefix}{plain}Configuration reloaded in {hl}{time}ms{plain}.";

        @Comment("Sent when a reload fails (use {time})")
        private String reloadFailure = "{prefix}{error}Reload failed in {time}ms. "
                + "Check the console for details.";

        @Comment("Sent when changed labels need a restart")
        private String reloadRestartRequired = "{prefix}{hl}Reload complete. "
                + "{plain}Command label changes need a server restart.";

        @Comment("Help header (use {version})")
        private String helpHeader = "\\n{prefix}{brand}JuicyTPA {version} Help:";

        @Comment("One help entry (use {syntax} and {description})")
        private String helpEntryFormat = "{brand}» {hl}{syntax}{plain}: {description}";

        @Comment("Version command output (use {version} and {authors})")
        private String versionSuccess = "\n{prefix}{brand}Version {hl}{version}"
                + "\n{brand}» {hl}Authors: {plain}{authors}"
                + "\n{brand}» {hl}Links: {modrinth}{plain} | {github}{plain} | {donate}";

        @Comment("Update notice (use {latest}, {current}, {url} and {modrinth-url})")
        private String updateAvailable = "\\n{prefix}{hl}JuicyTPA {latest}{brand} is available"
                + " (running {hl}{current}{brand})."
                + "\\n{brand}» {hl}Update: {modrinth} {plain}| {github} {plain}| {discord}";

        @Comment("Sent when the sender lacks a permission (use {permission})")
        private String noPermission = "{prefix}{error}You do not have the permission "
                + "{hl}'{permission}'{error} to use this command.";

        @Comment("Sent when a target selector matched no players")
        private String noTargets = "{prefix}{error}No players matched that target selector.";

        @Comment("Sent when console runs a command without a target selector")
        private String consoleNeedsTarget = "{prefix}{error}Console must specify a target.";
    }

    @Comment("Prefix prepended to every message, trailing space included")
    private String prefix = "<#a6a6a6>[<gradient:#44e63e:#60e95b><bold>J</bold>uicy<bold>TPA</bold>"
            + "</gradient>]</#a6a6a6> ";

    @Comment("Core plugin messages")
    private Core core = new Core();

    @Comment("Teleport request messages")
    private Tpa tpa = new Tpa();

    @Getter
    @Setter
    @SuppressWarnings("FieldMayBeFinal")
    public static class Tpa extends OkaeriConfig {

        @Comment("Request notice sent to the receiver (use {body}, {time}, {accept} and {reject})")
        private String requestReceived = "\\n{prefix}\\n{body}"
                + "\\n{plain}Accept within {hl}{time}{plain}."
                + "\\n{accept} {plain}| {reject}";

        @Comment("Receiver notice body for /tpa requests (use {player})")
        private String requestAskBody = "{hl}{player} {plain}has sent you a {hl}TP request{plain}.";

        @Comment("Receiver notice body for /tpahere requests (use {player})")
        private String requestHereBody = "{hl}{player} {plain}wants you to {hl}TP to them{plain}.";

        @Comment("Request actionbar shown to the receiver (use {player})")
        private String requestReceivedActionbar = "{brand}{hl}{player} {brand}wants to TP!";

        @Comment("Clickable accept button text in the request notice, shown in {ac}")
        private String acceptText = "[✔ Accept]";

        @Comment("Clickable reject button text in the request notice, shown in {rc}")
        private String rejectText = "[✕ Reject]";

        @Comment("Request confirmation sent to the sender (use {player} and {time})")
        private String requestSent = "{prefix}{sc}Sent TP request to {hl}{player}{sc}."
                + " They have {hl}{time}{sc} to accept.";

        @Comment("Request actionbar shown to the sender (use {player})")
        private String requestSentActionbar = "{sc}Sent TP request to {hl}{player}{sc}.";

        @Comment("Accept confirmation sent to the receiver (use {player})")
        private String acceptReceiver = "{prefix}{ac}Accepted TP request from {hl}{player}{ac}.";

        @Comment("Accept notice sent to the sender (use {player})")
        private String acceptSender = "{prefix}{ac}TP request accepted by {hl}{player}{ac}.";

        @Comment("Deny confirmation sent to the receiver (use {player})")
        private String denyReceiver = "{prefix}{rc}Denied TP request from {hl}{player}{rc}.";

        @Comment("Deny notice sent to the sender (use {player})")
        private String denySender = "{prefix}{rc}TP request denied by {hl}{player}{rc}.";

        @Comment("Deny actionbar shown to the receiver (use {player})")
        private String denyReceiverActionbar = "{rc}Denied {hl}{player}{rc}.";

        @Comment("Deny actionbar shown to the sender (use {player})")
        private String denySenderActionbar = "{rc}Denied by {hl}{player}{rc}.";

        @Comment("Timeout notice sent to the sender (use {player})")
        private String timeoutSender = "{prefix}{error}TP request to {hl}{player}{error} timed out.";

        @Comment("Timeout notice sent to the receiver (use {player})")
        private String timeoutReceiver = "{prefix}{error}TP request from {hl}{player}{error} timed out.";

        @Comment("Timeout actionbar shown to the sender (use {player})")
        private String timeoutSenderActionbar = "{error}Request to {hl}{player}{error} timed out.";

        @Comment("Timeout actionbar shown to the receiver (use {player})")
        private String timeoutReceiverActionbar = "{error}Request from {hl}{player}{error} timed out.";

        @Comment("Cancel confirmation sent to the sender (use {player})")
        private String cancelSender = "{prefix}{plain}Cancelled TP request to {hl}{player}{plain}.";

        @Comment("Cancel notice sent to the receiver (use {player})")
        private String cancelReceiver = "{prefix}{plain}TP request from {hl}{player}{plain} was cancelled.";

        @Comment("Sent when the sender changes their TPA mode (use {mode})")
        private String modeSet = "{prefix}{sc}TPA mode set to {hl}{mode}{sc}.";

        @Comment("Sent when the mode name is invalid (use {input})")
        private String modeInvalid = "{prefix}{error}Unknown TPA mode {hl}{input}{error}.";

        @Comment("Display name of the normal TPA mode, used in messages and placeholders")
        private String modeNormal = "Normal";

        @Comment("Display name of the block-all TPA mode, used in messages and placeholders")
        private String modeBlockAll = "<#f5c542>Block All";

        @Comment("Display name of the accept-all TPA mode, used in messages and placeholders")
        private String modeAcceptAll = "<#f5535e>Accept All";

        @Comment("Sent when the sender targets themselves")
        private String selfRequest = "{prefix}{error}You cannot send a TP request to yourself.";

        @Comment("Sent when the sender already has a pending request and concurrency is off")
        private String alreadyPending = "{prefix}{error}You already have a pending TP request.";

        @Comment("Sent when the receiver blocks requests (use {player})")
        private String receiverBlocked = "{prefix}{hl}{player} {error}is not accepting TP requests.";

        @Comment("Sent when the receiver has no pending requests to answer")
        private String noPending = "{prefix}{error}You have no pending TP requests.";

        @Comment("Sent when the receiver has no request from that player (use {player})")
        private String noPendingFrom = "{prefix}{error}You have no TP request from {hl}{player}{error}.";

        @Comment("Sent when argless accept/deny matches several requests")
        private String multiplePending = "{prefix}{error}You have multiple pending TP requests."
                + " Specify a player.";

        @Comment("Sent when the sender has no outgoing requests to cancel")
        private String noOutgoing = "{prefix}{error}You have no outgoing TP requests.";

        @Comment("Sent when the sender has no request to that player (use {player})")
        private String noOutgoingTo = "{prefix}{error}You have no TP request to {hl}{player}{error}.";

        @Comment("Sent when a selector resolves to anything but one player")
        private String notSingle = "{prefix}{error}Specify exactly one player.";

        @Comment("Sent when a removed selector (@s or @p) is used")
        private String selectorUnsupported = "{prefix}{error}That selector is not supported. Name a player instead.";

        @Comment("Sent when console runs a player-only TPA command")
        private String playerOnly = "{prefix}{error}Only players can use this command.";

        @Comment("Sent when the receiver blocked the sender (use {player})")
        private String requestBlocked = "{prefix}{hl}{player} {error}has blocked you.";

        @Comment("Sent when the sender blocks a player (use {player})")
        private String blockAdded = "{prefix}{sc}Blocked TP requests from {hl}{player}{sc}.";

        @Comment("Sent when the sender unblocks a player (use {player})")
        private String blockRemoved = "{prefix}{sc}Unblocked TP requests from {hl}{player}{sc}.";

        @Comment("Sent when the sender tries to block themselves")
        private String blockSelf = "{prefix}{error}You cannot block yourself.";

        @Comment("Sent when a bulk request finishes (use {count})")
        private String tpallSent = "{prefix}{sc}Sent TP requests to {hl}{count} players{sc}.";

        @Comment("Sent when a pending request is auto-cancelled by another accept (use {player})")
        private String autoCancelled = "{prefix}{plain}TP request to {hl}{player}{plain} was cancelled.";

        @Comment("Request type word for /tpa, used via {type} in warnings")
        private String requestTypeAsk = "TP request";

        @Comment("Request type word for /tpahere, used via {type} in warnings")
        private String requestTypeHere = "TP to them";

        @Comment("Warning sent before requesting from an AFK player (use {player} and {type})")
        private String warnAfk = "{prefix}{plain}Looks like {hl}{player} {plain}has been {lh}AFK {plain}for a while. "
                + "{hl}Re-send {plain}to confirm {hl}{type}{plain}.";

        @Comment("Warning sent before requesting from a frequent canceller (use {player} and {type})")
        private String warnCanceller = "{prefix}{plain}Looks like {hl}{player} {plain}keeps rejecting "
                + "{hl}TP requests{plain} lately. {hl}Re-send {plain}to confirm {hl}{type}{plain}.";

        @Comment("Nudge shown after rejecting several requests (use {button})")
        private String nudgeCancel = "\\n{prefix}{plain}Looks like you've rejected multiple "
                + "{hl}TP requests{plain} now.\\n{button}";

        @Comment("Nudge shown after missing several requests while away (use {button})")
        private String nudgeAfk = "\\n{prefix}{plain}Looks like you've missed multiple "
                + "{hl}TP requests{plain} while away.\\n{button}";

        @Comment("Nudge shown when one sender keeps requesting (use {player} and {button})")
        private String nudgeHarass = "\\n{prefix}{plain}Looks like {hl}{player} {plain}keeps sending you "
                + "{hl}TP requests{plain}.\\n{button}";

        @Comment("Block-all button text in nudges, wrapped with its click action")
        private String warnBlockAllButton = "{lh}[💻 Block All]";

        @Comment("Block-player button text in nudges, wrapped with its click action (use {player})")
        private String warnBlockPlayerButton = "{lh}[👤 Block {player}]";
    }
}
