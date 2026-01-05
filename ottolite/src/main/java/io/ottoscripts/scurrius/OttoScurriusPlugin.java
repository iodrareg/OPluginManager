package io.ottoscripts.scurrius;

import com.google.inject.Provides;
import io.ottoscripts.util.ActionScheduler;
import io.ottoscripts.util.ClickIndicatorOverlay;
import io.ottoscripts.util.PrayerFlickHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Slf4j
@PluginDescriptor(
    name = "OttoScurrius",
    description = "Scurrius boss helper plugin",
    tags = {"scurrius", "boss", "prayer", "otto", "ottolite"},
    enabledByDefault = true,
    hidden = true
)
public class OttoScurriusPlugin extends Plugin {
    // Scurrius NPC IDs
    private static final int SCURRIUS_SOLO_ID = 7222;
    private static final int SCURRIUS_GROUP_ID = 7221;
    private static final Set<Integer> SCURRIUS_IDS = new HashSet<>(Arrays.asList(SCURRIUS_SOLO_ID, SCURRIUS_GROUP_ID));

    // Scurrius Projectile IDs
    // Ranged attack (fur ball) - Use Protect from Missiles
    private static final int SCURRIUS_RANGED_PROJECTILE = 2642;
    // Magic attack (blue lightning) - Use Protect from Magic
    private static final int SCURRIUS_MAGIC_PROJECTILE = 2640;
    // Ground attack graphic ID - highlight tile
    private static final int SCURRIUS_GROUND_ATTACK_GRAPHIC = 2644;

    @Inject
    private Client client;

    @Inject
    private OttoScurriusConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private SpriteManager spriteManager;

    @Getter
    private boolean running = false;

    private ActionScheduler scheduler;
    private ClickIndicatorOverlay clickOverlay;
    private PrayerFlickHandler prayerFlickHandler;
    private ScurriusTileOverlay tileOverlay;

    // Track incoming projectile - ticks until it lands
    private int ticksUntilProjectileLands = 0;
    private Prayer incomingAttackPrayer = null;

    // Track dangerous ground tiles
    @Getter
    private final Set<WorldPoint> dangerousTiles = new HashSet<>();

    @Override
    protected void startUp() throws Exception {
        log.info("OttoScurrius started");
        clickOverlay = new ClickIndicatorOverlay(client, spriteManager);
        scheduler = new ActionScheduler();
        scheduler.setOnActionCallback(clickOverlay::showClick);
        prayerFlickHandler = new PrayerFlickHandler(client);
        tileOverlay = new ScurriusTileOverlay(client, this);
        overlayManager.add(clickOverlay);
        overlayManager.add(tileOverlay);
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("OttoScurrius stopped");
        running = false;
        resetState();
        if (clickOverlay != null) {
            overlayManager.remove(clickOverlay);
            clickOverlay = null;
        }
        if (tileOverlay != null) {
            overlayManager.remove(tileOverlay);
            tileOverlay = null;
        }
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
        if (prayerFlickHandler != null) {
            prayerFlickHandler.stopFlicking();
            prayerFlickHandler = null;
        }
    }

    private void resetState() {
        ticksUntilProjectileLands = 0;
        incomingAttackPrayer = null;
        dangerousTiles.clear();
        if (prayerFlickHandler != null) {
            prayerFlickHandler.stopFlicking();
            prayerFlickHandler.deactivateProtectionPrayers();
        }
    }

    public void setRunning(boolean running) {
        this.running = running;
        if (running) {
            log.info("OttoScurrius enabled");
        } else {
            log.info("OttoScurrius disabled");
            resetState();
        }
    }

    @Subscribe
    public void onProjectileMoved(ProjectileMoved event) {
        if (!running || (!config.autoPrayer() && !config.oneTickPrayer())) {
            return;
        }

        Projectile projectile = event.getProjectile();
        int projectileId = projectile.getId();

        Prayer newPrayer = null;
        int travelTicks = 0;

        if (projectileId == SCURRIUS_MAGIC_PROJECTILE) {
            newPrayer = Prayer.PROTECT_FROM_MAGIC;
            travelTicks = calculateTravelTicks(projectile);
            log.debug("Scurrius magic projectile detected - lands in {} ticks", travelTicks);
        } else if (projectileId == SCURRIUS_RANGED_PROJECTILE) {
            newPrayer = Prayer.PROTECT_FROM_MISSILES;
            travelTicks = calculateTravelTicks(projectile);
            log.debug("Scurrius ranged projectile detected - lands in {} ticks", travelTicks);
        }

        if (newPrayer != null && travelTicks > 0) {
            incomingAttackPrayer = newPrayer;
            ticksUntilProjectileLands = travelTicks;
        }
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated event) {
        if (!running) {
            return;
        }

        GraphicsObject graphicsObject = event.getGraphicsObject();
        if (graphicsObject.getId() == SCURRIUS_GROUND_ATTACK_GRAPHIC) {
            LocalPoint localPoint = graphicsObject.getLocation();
            if (localPoint != null) {
                WorldPoint worldPoint = WorldPoint.fromLocal(client, localPoint);
                dangerousTiles.add(worldPoint);
                log.debug("Ground attack at {}", worldPoint);
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!running) {
            return;
        }

        // Clear old dangerous tiles (graphics objects expire after a few ticks)
        updateDangerousTiles();

        // Check if we're near Scurrius
        if (!isNearScurrius()) {
            if (prayerFlickHandler != null && prayerFlickHandler.isFlicking()) {
                prayerFlickHandler.stopFlicking();
            }
            ticksUntilProjectileLands = 0;
            incomingAttackPrayer = null;
            return;
        }

        // Determine which prayer we need this tick
        Prayer prayerThisTick;

        if (ticksUntilProjectileLands > 0) {
            // Projectile is in the air - pray against it
            prayerThisTick = incomingAttackPrayer;
            ticksUntilProjectileLands--;

            if (ticksUntilProjectileLands == 0) {
                // Projectile has landed
                log.debug("Projectile landed - switching back to melee protection");
                incomingAttackPrayer = null;
            }
        } else {
            // No projectile incoming - protect from melee
            prayerThisTick = Prayer.PROTECT_FROM_MELEE;
        }

        // Handle 1-tick prayer flicking
        if (config.oneTickPrayer() && prayerFlickHandler != null) {
            if (prayerFlickHandler.getActivePrayer() != prayerThisTick) {
                prayerFlickHandler.switchPrayer(prayerThisTick);
            }

            if (!prayerFlickHandler.isEnabled()) {
                prayerFlickHandler.startFlicking(prayerThisTick);
            }

            prayerFlickHandler.flick();
        }
        // Handle auto prayer (not 1-tick)
        else if (config.autoPrayer()) {
            if (prayerThisTick == Prayer.PROTECT_FROM_MAGIC) {
                prayerFlickHandler.deactivatePrayer(Prayer.PROTECT_FROM_MISSILES);
                prayerFlickHandler.deactivatePrayer(Prayer.PROTECT_FROM_MELEE);
            } else if (prayerThisTick == Prayer.PROTECT_FROM_MISSILES) {
                prayerFlickHandler.deactivatePrayer(Prayer.PROTECT_FROM_MAGIC);
                prayerFlickHandler.deactivatePrayer(Prayer.PROTECT_FROM_MELEE);
            } else if (prayerThisTick == Prayer.PROTECT_FROM_MELEE) {
                prayerFlickHandler.deactivatePrayer(Prayer.PROTECT_FROM_MAGIC);
                prayerFlickHandler.deactivatePrayer(Prayer.PROTECT_FROM_MISSILES);
            }
            prayerFlickHandler.activatePrayer(prayerThisTick);
        }
    }

    /**
     * Calculate ticks until projectile lands based on remaining cycles.
     */
    private int calculateTravelTicks(Projectile projectile) {
        int remainingCycles = projectile.getRemainingCycles();
        // Each game tick is 30 client cycles
        return (remainingCycles / 30) + 1;
    }

    /**
     * Update dangerous tiles - remove tiles where graphics have expired.
     */
    private void updateDangerousTiles() {
        if (dangerousTiles.isEmpty()) {
            return;
        }

        Set<WorldPoint> activeGraphicsLocations = new HashSet<>();
        for (GraphicsObject go : client.getGraphicsObjects()) {
            if (go.getId() == SCURRIUS_GROUND_ATTACK_GRAPHIC) {
                LocalPoint lp = go.getLocation();
                if (lp != null) {
                    activeGraphicsLocations.add(WorldPoint.fromLocal(client, lp));
                }
            }
        }

        // Remove tiles that no longer have active graphics
        dangerousTiles.retainAll(activeGraphicsLocations);
    }

    /**
     * Check if the player is near Scurrius.
     */
    private boolean isNearScurrius() {
        for (NPC npc : client.getNpcs()) {
            if (npc != null && SCURRIUS_IDS.contains(npc.getId())) {
                return true;
            }
        }
        return false;
    }

    @Provides
    OttoScurriusConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(OttoScurriusConfig.class);
    }
}
