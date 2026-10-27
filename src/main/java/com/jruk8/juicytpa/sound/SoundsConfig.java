package com.jruk8.juicytpa.sound;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Header;
import eu.okaeri.configs.annotation.Comment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Header({
        "################################",
        "#            Sounds            #",
        "################################",
        "",
        "Sounds configuration for plugin sounds.",
        "Each entry supports enabled, a namespaced sound key, pitch and volume.",
        "Sounds are resolved against the server's sound registry, see",
        "Mudkipdev's explorer: https://mudkipdev.github.io/minecraft-sound-explorer/",
        ""
})
public class SoundsConfig extends OkaeriConfig {

    @Comment("Neutral feedback for ordinary commands, heard by the executor and every affected player.")
    private Sound neutral = new Sound(true, "block.cobweb.place", 1.2f, 1.0f);

    @Comment("Sound played for reloads, help and version output.")
    private Sound celebration = new Sound(true, "block.enchantment_table.use", 1.2f, 0.7f);

    @Comment("Sound played to the receiver when a TP request arrives.")
    private Sound ask = new Sound(true, "entity.cat.beg_for_food", 1.2f, 1.0f);

    @Comment("Sound played to the party that stays put when a TP request is accepted.")
    private Sound accept = new Sound(true, "block.cherry_wood_pressure_plate.click_on", 1.2f, 1.0f);

    @Comment("Sound played to the teleporter at the destination after a successful teleport.")
    private Sound teleport = new Sound(true, "entity.player.teleport", 1.2f, 1.0f);

    @Comment("Sound played to the sender when a TP request is denied.")
    private Sound reject = new Sound(true, "entity.cat.purreow", 0.7f, 1.0f);

    @Comment("Sound played to both parties when a TP request times out.")
    private Sound timeout = new Sound(true, "entity.cat.purreow", 0.7f, 1.0f);

    @Comment("Sound played when a command fails (invalid input, missing permission, failed reload).")
    private Sound angry = new Sound(true, "block.chiseled_bookshelf.insert", 1.1f, 1.0f);

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sound extends OkaeriConfig {

        @Comment("Whether this sound plays at all.")
        private boolean enabled = true;

        @Comment("Namespaced sound key, e.g. entity.breeze.land or minecraft:entity.breeze.land.")
        private String sound = "block.note_block.pling";

        @Comment("Sound pitch, usually between 0.5 and 2.0.")
        private float pitch = 1.0f;

        @Comment("Sound volume, usually between 0.0 and 1.0.")
        private float volume = 1.0f;

        public Sound(boolean enabled, String sound) {
            this(enabled, sound, 1.0f, 1.0f);
        }
    }
}
