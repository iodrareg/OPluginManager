package io.ottoscripts.gauntlet;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ottogauntlet")
public interface OttoGauntletConfig extends Config {
    @ConfigItem(
        keyName = "enabled",
        name = "Enabled",
        description = "Enable OttoGauntlet"
    )
    default boolean enabled() {
        return false;
    }

    @ConfigItem(
        keyName = "autoPrayer",
        name = "Auto Prayer",
        description = "Automatically switch prayers based on Hunllef attack cycle"
    )
    default boolean autoPrayer() {
        return false;
    }

    @ConfigItem(
        keyName = "oneTickPrayer",
        name = "1 Tick Prayer",
        description = "Enable 1 tick prayer flicking"
    )
    default boolean oneTickPrayer() {
        return false;
    }
}
