package io.ottoscripts.agility;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("ottoagility")
public interface OttoAgilityConfig extends Config {
    @ConfigItem(
        keyName = "enabled",
        name = "Enabled",
        description = "Enable OttoAgility"
    )
    default boolean enabled() {
        return false;
    }

    @Range(
        min = 1,
        max = 100
    )
    @ConfigItem(
        keyName = "stepsToAutomate",
        name = "Steps to Automate",
        description = "Number of agility steps to automate",
        warning = "Setting this value too high may cause macroing detection."
    )
    default int stepsToAutomate() {
        return 1;
    }
}
