package com.jruk8.juicytpa.message;

import java.util.HashMap;
import java.util.Map;

/**
 * The plugin's link buttons ({@code {modrinth}}, {@code {github}},
 * {@code {discord}}, {@code {donate}}), injected per recipient by the
 * messenger so labels and tooltips follow the reader's language.
 * <p>
 * Templates live in messages.yml, so the defaults stay readable and
 * translators can reword them; only the URLs live in code. Every
 * link shares one clickable shape: the label in its own color,
 * opening the page on click, with a fully colored tooltip.
 */
public final class PluginLinks {

    public static final String MODRINTH_URL = "https://modrinth.com/project/juicytpa";
    public static final String GITHUB_URL = "https://github.com/jruk8/JuicyTPA";
    public static final String DISCORD_URL = "https://discord.com/invite/hkWmCVmWDC";
    public static final String DONATE_URL = "https://ko-fi.com/jruk";

    private PluginLinks() {
    }

    /**
     * @param messages the recipient's bundle
     * @return the link placeholders and their raw MiniMessage buttons
     */
    public static Map<String, String> buttons(MessagesConfig messages) {
        var core = messages.getCore();
        Map<String, String> buttons = new HashMap<>();
        buttons.put("{modrinth}", core.getLinkModrinth().replace("{url}", MODRINTH_URL));
        buttons.put("{github}", core.getLinkGithub().replace("{url}", GITHUB_URL));
        buttons.put("{discord}", core.getLinkDiscord().replace("{url}", DISCORD_URL));
        buttons.put("{donate}", core.getLinkDonate().replace("{url}", DONATE_URL));
        return buttons;
    }
}
