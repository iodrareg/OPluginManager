package io.ottoscripts.thieving;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("ottothieving")
public interface OttoThievingConfig extends Config {
    @ConfigItem(
        keyName = "enabled",
        name = "Enabled",
        description = "Enable OttoThieving"
    )
    default boolean enabled() {
        return false;
    }

    @ConfigItem(
        keyName = "pickpocketUntilFailure",
        name = "Pickpocket until failure",
        description = "Continue pickpocketing until you fail"
    )
    default boolean pickpocketUntilFailure() {
        return false;
    }

    @ConfigItem(
        keyName = "autoOpenPouches",
        name = "Auto open money pouch",
        description = "Automatically open coin pouches at a certain amount"
    )
    default boolean autoOpenPouches() {
        return false;
    }

    @Range(
        min = 1,
        max = 28
    )
    @ConfigItem(
        keyName = "pouchThreshold",
        name = "Pouch threshold",
        description = "Number of pouches before auto-opening"
    )
    default int pouchThreshold() {
        return 28;
    }

    @ConfigItem(
        keyName = "autoShadowVeil",
        name = "Autocast Shadow Veil",
        description = "Automatically cast Shadow Veil spell"
    )
    default boolean autoShadowVeil() {
        return false;
    }

    @ConfigItem(
        keyName = "autoEat",
        name = "Auto eat food",
        description = "Automatically eat food when health drops below threshold"
    )
    default boolean autoEat() {
        return false;
    }

    @Range(
        min = 1,
        max = 99
    )
    @ConfigItem(
        keyName = "eatHealthPercent",
        name = "Eat at health %",
        description = "Health percentage to eat at"
    )
    default int eatHealthPercent() {
        return 50;
    }
}
