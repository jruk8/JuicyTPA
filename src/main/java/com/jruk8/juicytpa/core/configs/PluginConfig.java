package com.jruk8.juicytpa.core.configs;

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
            "{error}, {ac}, {rc} and {sc}. Values are MiniMessage color tags."
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
    }

    @Getter
    @Setter
    @SuppressWarnings("FieldMayBeFinal")
    public static class Concurrency extends OkaeriConfig {

        @Comment("Allow players to hold several pending requests at once.")
        private boolean allowConcurrent = false;

        @Comment({
                "Cancel the receiver's other pending requests when one is",
                "accepted. Only applies when allowConcurrent is true."
        })
        private boolean cancelExistingOnAccept = true;
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

        @Comment("Standalone /tpamode root command and its extra labels.")
        private Channel tpamode = new Channel(List.of("tpam", "tpmode", "tpm"));

        @Comment("Standalone /tpablock root command and its extra labels.")
        private Channel tpablock = new Channel(List.of());

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
        private String errorColor = "<#e63e3e>";

        @Comment("MiniMessage color tag for accept prompts, used via {ac}.")
        private String acceptColor = "<#7be63e>";

        @Comment("MiniMessage color tag for reject prompts, used via {rc}.")
        private String rejectColor = "<#e63e5a>";

        @Comment("MiniMessage color tag for sender feedback, used via {sc}.")
        private String sentColor = "<#44e63e>";
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
