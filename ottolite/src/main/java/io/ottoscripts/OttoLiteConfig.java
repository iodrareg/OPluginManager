package io.ottoscripts;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ottolite")
public interface OttoLiteConfig extends Config {
    @ConfigItem(
        keyName = "featureExample",
        name = "Example Feature",
        description = "An example feature toggle"
    )
    default boolean featureExample() {
        return false;
    }
}
