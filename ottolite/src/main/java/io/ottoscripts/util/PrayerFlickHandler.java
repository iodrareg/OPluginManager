package io.ottoscripts.util;

import com.example.InteractionApi.PrayerInteraction;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Prayer;

/**
 * Generic handler for 1-tick prayer flicking.
 * Can be used by any plugin that needs prayer flicking functionality.
 *
 * 1-tick flicking works by toggling prayer OFF then ON within the same game tick,
 * which resets the prayer drain timer while maintaining protection.
 */
@Slf4j
public class PrayerFlickHandler {
    private final Client client;

    @Getter
    @Setter
    private boolean enabled = false;

    @Getter
    @Setter
    private Prayer activePrayer = null;

    public PrayerFlickHandler(Client client) {
        this.client = client;
    }

    /**
     * Perform a 1-tick flick: if prayer is on, toggle OFF then ON.
     * If prayer is off, just toggle ON.
     * Call this at the start of each game tick.
     */
    public void flick() {
        if (!enabled || activePrayer == null) {
            return;
        }

        // If prayer is already active, toggle it off first (then on)
        if (client.isPrayerActive(activePrayer)) {
            PrayerInteraction.togglePrayer(activePrayer);
            log.debug("1-tick flick: {} OFF", activePrayer.name());
        }

        // Toggle on
        PrayerInteraction.togglePrayer(activePrayer);
        log.debug("1-tick flick: {} ON", activePrayer.name());
    }

    /**
     * Set the prayer to flick and enable flicking.
     *
     * @param prayer the prayer to flick
     */
    public void startFlicking(Prayer prayer) {
        this.activePrayer = prayer;
        this.enabled = true;
        log.info("Started 1-tick flicking with {}", prayer.name());
    }

    /**
     * Stop flicking and ensure prayer is off.
     */
    public void stopFlicking() {
        if (activePrayer != null) {
            PrayerInteraction.setPrayerState(activePrayer, false);
        }
        this.enabled = false;
        this.activePrayer = null;
        log.info("Stopped 1-tick flicking");
    }

    /**
     * Switch to a different prayer for flicking.
     *
     * @param prayer the new prayer to flick
     */
    public void switchPrayer(Prayer prayer) {
        if (activePrayer != null && activePrayer != prayer && client.isPrayerActive(activePrayer)) {
            PrayerInteraction.setPrayerState(activePrayer, false);
        }
        this.activePrayer = prayer;
        log.debug("Switched flicking prayer to {}", prayer.name());
    }

    /**
     * Immediately activate a prayer (for non-flicking auto prayer).
     *
     * @param prayer the prayer to activate
     */
    public void activatePrayer(Prayer prayer) {
        if (!client.isPrayerActive(prayer)) {
            PrayerInteraction.setPrayerState(prayer, true);
            log.debug("Activated prayer: {}", prayer.name());
        }
    }

    /**
     * Deactivate a prayer.
     *
     * @param prayer the prayer to deactivate
     */
    public void deactivatePrayer(Prayer prayer) {
        if (client.isPrayerActive(prayer)) {
            PrayerInteraction.setPrayerState(prayer, false);
            log.debug("Deactivated prayer: {}", prayer.name());
        }
    }

    /**
     * Deactivate all protection prayers.
     */
    public void deactivateProtectionPrayers() {
        deactivatePrayer(Prayer.PROTECT_FROM_MAGIC);
        deactivatePrayer(Prayer.PROTECT_FROM_MISSILES);
        deactivatePrayer(Prayer.PROTECT_FROM_MELEE);
    }

    /**
     * Check if the handler is currently flicking.
     *
     * @return true if flicking is active
     */
    public boolean isFlicking() {
        return enabled && activePrayer != null;
    }
}
