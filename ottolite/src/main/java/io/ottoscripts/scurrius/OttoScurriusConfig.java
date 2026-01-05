package io.ottoscripts.scurrius;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ottoscurrius")
public interface OttoScurriusConfig extends Config {
    @ConfigItem(
        keyName = "enabled",
        name = "Enabled",
        description = "Enable OttoScurrius"
    )
    default boolean enabled() {
        return false;
    }

    @ConfigItem(
        keyName = "autoPrayer",
        name = "Auto Prayer",
        description = "Automatically switch prayers"
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
