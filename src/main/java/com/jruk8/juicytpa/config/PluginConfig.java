package com.jruk8.juicytpa.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Header;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@SuppressWarnings("FieldMayBeFinal")
@Header({
        "#      ██╗██╗   ██╗██╗ ██████╗██╗   ██╗    ████████╗██████╗  █████╗",
        "#      ██║██║   ██║██║██╔════╝╚██╗ ██╔╝    ╚══██╔══╝██╔══██╗██╔══██╗",
        "#      ██║██║   ██║██║██║      ╚████╔╝        ██║   ██████╔╝███████║",
        "# ██   ██║██║   ██║██║██║       ╚██╔╝         ██║   ██╔═══╝ ██╔══██║",
        "# ╚█████╔╝╚██████╔╝██║╚██████╗   ██║          ██║   ██║     ██║  ██║",
        "#  ╚════╝  ╚═════╝ ╚═╝ ╚═════╝   ╚═╝          ╚═╝   ╚═╝     ╚═╝  ╚═╝",
        "",
        "# 🌀 A modern 26.x plugin for /tpa and /tpahere.",
        ""
})
public class PluginConfig extends OkaeriConfig {

    @Comment("GitHub update checker settings.")
    private UpdateChecker updateChecker = new UpdateChecker();

    @Comment({
            "Extra labels for the /juicytpa root command (shortcuts like /jtpa).",
            "Applies on restart."
    })
    private List<String> aliases = List.of("jtpa", "jtp", "jt");

    @Comment({
            "SQLite storage settings.",
            "The database file is created inside the plugin's data folder."
    })
    private Storage storage = new Storage();

    @Comment({
            "Teleport request settings: expiry, concurrency and per-event",
            "chat/actionbar feedback."
    })
    private Tpa tpa = new Tpa();

    @Comment({
            "Standalone root commands: each channel registers (or hides) its",
            "root, with extra labels under 'aliases' (also reachable under",
            "/juicytpa). Applies on restart; /juicytpa reload reports changed",
            "toggles instead."
    })
    private Commands commands = new Commands();

    @Comment({
            "Color palette used by every message via {plain}, {brand}, {hl},",
            "{error}, {ac}, {rc}, {sc}, {lh} and {wn}. Values are MiniMessage color tags."
    })
    private Colors colors = new Colors();

    @Comment({
            "Anonymous usage statistics via bStats (open-source, no personal data or IP addresses).",
            "Requires a server restart to take effect."
    })
    private Metrics metrics = new Metrics();

    @Getter
    @Setter
    @SuppressWarnings("FieldMayBeFinal")
    public static class Storage extends OkaeriConfig {

        @Comment("Name of the SQLite database file created in the plugin's data folder.")
        private String dbFile = "juicytpa.db";
    }

    @Getter
    @Setter
    @SuppressWarnings("FieldMayBeFinal")
    public static class Tpa extends OkaeriConfig {

        @Comment("How long a request stays pending before it times out, in seconds.")
        private int requestDuration = 60;

        @Comment("How overlapping requests behave.")
        private Concurrency concurrency = new Concurrency();

        @Comment("Chat message and actionbar sent to the request receiver.")
        private Feedback receiverFeedback = new Feedback();

        @Comment("Chat message and actionbar sent to the request sender.")
        private Feedback senderFeedback = new Feedback();

        @Comment("Chat message and actionbar sent when a request is denied.")
        private Feedback rejectFeedback = new Feedback();

        @Comment("Chat message and actionbar sent when a request times out.")
        private Feedback timeoutFeedback = new Feedback();

        @Comment({
                "AFK and canceller warnings: thresholds, resend confirmations,",
                "receiver nudges and harassment detection."
        })
        private Warnings warnings = new Warnings();

        @Comment("Cooldown between outgoing /tpa and /tpahere requests.")
        private Cooldown cooldown = new Cooldown();
    }

    @Getter
    @Setter
    @SuppressWarnings("FieldMayBeFinal")
    public static class Concurrency extends OkaeriConfig {

        @Comment("Allow players to hold several pending requests at once.")
        private boolean allowConcurrent = true;

        @Comment({
                "Maximum outgoing requests a player may hold at once.",
                "Zero or below means unlimited. /tpall bypasses this cap."
        })
        private int maxConcurrent = 3;

        @Comment({
                "Cancel the receiver's other pending requests when one is",
                "accepted. Only applies when allowConcurrent is true."
        })
        private boolean cancelExistingOnAccept = true;
    }

    @Getter
    @Setter
    @SuppressWarnings("FieldMayBeFinal")
    public static class Warnings extends OkaeriConfig {

        @Comment("Unanswered (expired) requests before a receiver counts as AFK.")
        private int afkMinUnanswered = 3;

        @Comment({
                "Cumulative seconds with any request pending before a receiver",
                "counts as AFK. Overlapping requests count once per second."
        })
        private int afkMinPendingSeconds = 180;

        @Comment("Denied requests before a receiver counts as a canceller.")
        private int cancellerMinDenied = 3;

        @Comment("Cumulative seconds denied requests sat pending before a receiver counts as a canceller.")
        private int cancellerMinPendingSeconds = 20;

        @Comment("Seconds a warning stays confirmable: re-send within this window to send anyway.")
        private int confirmWindowSeconds = 15;

        @Comment("Minimum seconds between warning nudges to the same player.")
        private int nudgeCooldownSeconds = 300;

        @Comment("Consecutive denies (no accept between) before the block-all nudge shows.")
        private int nudgeMinCancelStreak = 3;

        @Comment("Consecutive expiries (no accept between) before the block-all nudge shows.")
        private int nudgeMinExpiryStreak = 3;

        @Comment("Requests from one sender within the harassment window before the block-player nudge shows.")
        private int harassMinRequests = 3;

        @Comment("Sliding window in seconds for counting one sender's requests.")
        private int harassWindowSeconds = 600;
    }

    @Getter
    @Setter
    @SuppressWarnings("FieldMayBeFinal")
    public static class Cooldown extends OkaeriConfig {

        @Comment("Enforce a cooldown between /tpa and /tpahere sends.")
        private boolean enabled = true;

        @Comment({
                "Default cooldown in seconds when the player has no LuckPerms",
                "meta. Zero or below disables the cooldown."
        })
        private int defaultSeconds = 15;

        @Comment("LuckPerms meta key holding a per-player cooldown in seconds.")
        private String metaKey = "juicytpa-cooldown";
    }

    @Getter
    @Setter
    @SuppressWarnings("FieldMayBeFinal")
    public static class Feedback extends OkaeriConfig {

        @Comment("Send the chat message for this event.")
        private boolean message = true;

        @Comment("Send the actionbar for this event.")
        private boolean actionbar = true;
    }

    @Getter
    @Setter
    @SuppressWarnings("FieldMayBeFinal")
    public static class Commands extends OkaeriConfig {

        @Comment("Standalone /tpa root command and its extra labels.")
        private Channel tpa = new Channel(List.of("tpask"));

        @Comment("Standalone /tpahere root command and its extra labels.")
        private Channel tpahere = new Channel(List.of("tpah", "tphere", "tph"));

        @Comment("Standalone /tpaccept root command and its extra labels.")
        private Channel tpaccept = new Channel(List.of("tpacc"));

        @Comment("Standalone /tpadeny root command and its extra labels.")
        private Channel tpadeny = new Channel(List.of("tpad", "tpdeny", "tpd"));

        @Comment("Standalone /tpacancel root command and its extra labels.")
        private Channel tpacancel = new Channel(List.of("tpac", "tpcancel", "tpc"));

        @Comment("Standalone /tpalist root command and its extra labels.")
        private Channel tpalist = new Channel(List.of("tpal", "tplist", "tpl"));

        @Comment("Standalone /tpatrust root command and its extra labels.")
        private Channel tpatrust = new Channel(List.of("tpat", "tptrust", "tpt"));

        @Comment("Standalone /tpamode root command and its extra labels.")
        private Channel tpamode = new Channel(List.of("tpam", "tpmode", "tpm", "tpatoggle", "tptoggle"));

        @Comment("Standalone /tpablock root command and its extra labels.")
        private Channel tpablock =
                new Channel(List.of("tpab", "tpblock", "tpb", "tpaignore", "tpai", "tpignore", "tpi"));

        @Comment("Standalone /tpacceptall root command and its extra labels.")
        private Channel tpacceptall = new Channel(List.of("tpaauto", "tpauto"));

        @Comment("Standalone /tpablockall root command and its extra labels.")
        private Channel tpablockall = new Channel(List.of("tpaba", "tpblockall", "tpba"));

        @Comment("Standalone /tpall root command and its extra labels.")
        private Channel tpall = new Channel(List.of());
    }

    @Getter
    @Setter
    @SuppressWarnings("FieldMayBeFinal")
    public static class Channel extends OkaeriConfig {

        @Comment("Registers the standalone root command for this channel.")
        private boolean enabled = true;

        @Comment("Extra labels for the root command.")
        private List<String> aliases = List.of();

        public Channel() {
        }

        public Channel(List<String> aliases) {
            this.aliases = aliases;
        }
    }

    @Getter
    @Setter
    @SuppressWarnings("FieldMayBeFinal")
    public static class Colors extends OkaeriConfig {

        @Comment("MiniMessage color tag for normal message bodies, used via {plain}.")
        private String plainColor = "<#b9e0b8>";

        @Comment("MiniMessage color tag for brand accents, used via {brand}.")
        private String brandColor = "<#87db84>";

        @Comment("MiniMessage color tag for highlighted words, used via {hl}.")
        private String highlightColor = "<#ebf2eb>";

        @Comment("MiniMessage color tag for errors, used via {error}.")
        private String errorColor = "<#eb5757>";

        @Comment("MiniMessage color tag for accept prompts, used via {ac}.")
        private String acceptColor = "<#7be63e>";

        @Comment("MiniMessage color tag for reject prompts, used via {rc}.")
        private String rejectColor = "<#e63e5a>";

        @Comment("MiniMessage color tag for sender feedback, used via {sc}.")
        private String sentColor = "<#44e63e>";

        @Comment("MiniMessage color tag for warning highlights, used via {lh}.")
        private String lhColor = "<#bababa>";

        @Comment("MiniMessage color tag for warning bodies, used via {wn}.")
        private String warnColor = "<#e6db3e>";
    }

    @Getter
    @Setter
    @SuppressWarnings("FieldMayBeFinal")
    public static class Metrics extends OkaeriConfig {

        @Comment("Send anonymous statistics to bStats.")
        private boolean enabled = true;
    }

    @Getter
    @Setter
    @SuppressWarnings("FieldMayBeFinal")
    public static class UpdateChecker extends OkaeriConfig {

        @Comment({
                "Checks GitHub releases for new JuicyTPA versions on enable and on reload.",
                "The check runs asynchronously; findings go to the console and to",
                "players holding juicytpa.reload (on join and via /juicytpa version)."
        })
        private boolean enabled = true;
    }
}
