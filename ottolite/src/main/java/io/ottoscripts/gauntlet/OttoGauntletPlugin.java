package io.ottoscripts.gauntlet;

import com.google.inject.Provides;
import io.ottoscripts.util.ActionScheduler;
import io.ottoscripts.util.ClickIndicatorOverlay;
import io.ottoscripts.util.PrayerFlickHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.Projectile;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.*;

@Slf4j
@PluginDescriptor(
    name = "OttoGauntlet",
    description = "Gauntlet/Hunllef helper plugin",
    tags = {"gauntlet", "hunllef", "corrupted", "prayer", "otto", "ottolite"},
    enabledByDefault = true,
    hidden = true
)
public class OttoGauntletPlugin extends Plugin {
    // Crystalline Hunllef NPC IDs (normal Gauntlet)
    private static final Set<Integer> CRYSTALLINE_HUNLLEF_IDS = new HashSet<>(Arrays.asList(
        9021, 9022, 9023, 9024, 12123
    ));

    // Corrupted Hunllef NPC IDs (Corrupted Gauntlet)
    private static final Set<Integer> CORRUPTED_HUNLLEF_IDS = new HashSet<>(Arrays.asList(
        9035, 9036, 9037, 9038
    ));

    // Tornado NPC IDs
    private static final int TORNADO_CRYSTALLINE = 9025;
    private static final int TORNADO_CORRUPTED = 9039;
    private static final Set<Integer> TORNADO_IDS = new HashSet<>(Arrays.asList(
        TORNADO_CRYSTALLINE, TORNADO_CORRUPTED
    ));

    // Floor tile graphics IDs (damaging floor)
    // These are the graphic IDs for the floor tiles that damage players
    private static final int FLOOR_GRAPHIC_CRYSTALLINE = 1544;
    private static final int FLOOR_GRAPHIC_CORRUPTED = 1547;
    private static final Set<Integer> FLOOR_GRAPHICS = new HashSet<>(Arrays.asList(
        FLOOR_GRAPHIC_CRYSTALLINE, FLOOR_GRAPHIC_CORRUPTED
    ));

    // Hunllef Projectile IDs
    private static final int HUNLLEF_MAGE_PROJECTILE = 1707;
    private static final int HUNLLEF_RANGE_PROJECTILE = 1711;

    // Attacks before style switch
    private static final int ATTACKS_PER_SWITCH = 4;

    // Hunllef boss room region IDs
    private static final int HUNLLEF_ROOM_NORMAL = 7512;
    private static final int HUNLLEF_ROOM_CORRUPTED = 7768;

    @Inject
    private Client client;

    @Inject
    private OttoGauntletConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private SpriteManager spriteManager;

    @Getter
    private boolean running = false;

    private ActionScheduler scheduler;
    private ClickIndicatorOverlay clickOverlay;
    private PrayerFlickHandler prayerFlickHandler;
    private GauntletOverlay gauntletOverlay;
    private GauntletTileOverlay tileOverlay;

    // Track Hunllef
    @Getter
    private NPC hunllef = null;
    private int attackCount = 0;
    private boolean prayingMagic = true;
    private int lastProjectileTick = -1;
    private boolean inFight = false;

    // Track hazards
    @Getter
    private final Set<WorldPoint> tornadoTiles = new HashSet<>();
    @Getter
    private final Set<WorldPoint> dangerousFloorTiles = new HashSet<>();

    @Override
    protected void startUp() throws Exception {
        log.info("OttoGauntlet started");
        clickOverlay = new ClickIndicatorOverlay(client, spriteManager);
        scheduler = new ActionScheduler();
        scheduler.setOnActionCallback(clickOverlay::showClick);
        prayerFlickHandler = new PrayerFlickHandler(client);
        gauntletOverlay = new GauntletOverlay(client, this);
        tileOverlay = new GauntletTileOverlay(client, this);
        overlayManager.add(clickOverlay);
        overlayManager.add(gauntletOverlay);
        overlayManager.add(tileOverlay);
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("OttoGauntlet stopped");
        running = false;
        resetState();
        if (clickOverlay != null) {
            overlayManager.remove(clickOverlay);
            clickOverlay = null;
        }
        if (gauntletOverlay != null) {
            overlayManager.remove(gauntletOverlay);
            gauntletOverlay = null;
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
        hunllef = null;
        attackCount = 0;
        prayingMagic = true;
        lastProjectileTick = -1;
        inFight = false;
        tornadoTiles.clear();
        dangerousFloorTiles.clear();
        if (prayerFlickHandler != null) {
            prayerFlickHandler.stopFlicking();
            prayerFlickHandler.deactivateProtectionPrayers();
        }
    }

    public void setRunning(boolean running) {
        this.running = running;
        if (running) {
            log.info("OttoGauntlet enabled");
            findHunllef();
        } else {
            log.info("OttoGauntlet disabled");
            resetState();
        }
    }

    private void findHunllef() {
        for (NPC npc : client.getNpcs()) {
            if (npc != null && isHunllef(npc.getId())) {
                hunllef = npc;
                attackCount = 0;
                prayingMagic = true;
                log.info("Found Hunllef - defaulting to magic prayer, will sync on first attack");
                break;
            }
        }
    }

    private boolean isHunllef(int npcId) {
        return CRYSTALLINE_HUNLLEF_IDS.contains(npcId) || CORRUPTED_HUNLLEF_IDS.contains(npcId);
    }

    private boolean isTornado(int npcId) {
        return TORNADO_IDS.contains(npcId);
    }

    private boolean isInHunllefRoom() {
        Player player = client.getLocalPlayer();
        if (player == null) {
            return false;
        }
        int regionId = WorldPoint.fromLocalInstance(client, player.getLocalLocation()).getRegionID();
        return regionId == HUNLLEF_ROOM_NORMAL || regionId == HUNLLEF_ROOM_CORRUPTED;
    }

    private boolean isHunllefTargetingPlayer() {
        if (hunllef == null) {
            return false;
        }
        Player player = client.getLocalPlayer();
        return player != null && hunllef.getInteracting() == player;
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        if (!running) {
            return;
        }

        NPC npc = event.getNpc();
        if (isHunllef(npc.getId())) {
            hunllef = npc;
            attackCount = 0;
            prayingMagic = true;
            log.info("Hunllef spawned - defaulting to magic prayer, will sync on first attack");
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        if (!running) {
            return;
        }

        NPC npc = event.getNpc();
        if (isHunllef(npc.getId())) {
            log.info("Hunllef despawned");
            resetState();
        }
    }

    @Subscribe
    public void onProjectileMoved(ProjectileMoved event) {
        if (!running || hunllef == null) {
            return;
        }

        Projectile projectile = event.getProjectile();
        int projectileId = projectile.getId();

        boolean isMageAttack = projectileId == HUNLLEF_MAGE_PROJECTILE;
        boolean isRangeAttack = projectileId == HUNLLEF_RANGE_PROJECTILE;

        if (!isMageAttack && !isRangeAttack) {
            return;
        }

        // Only process each projectile once (when it first spawns)
        // ProjectileMoved fires every frame, so check if this is a new projectile
        int currentTick = client.getTickCount();
        if (currentTick == lastProjectileTick) {
            return;
        }
        lastProjectileTick = currentTick;

        attackCount++;

        // Mark that we're now in an active fight
        if (!inFight) {
            inFight = true;
            log.info("Hunllef fight started - first projectile detected");
        }

        // Sync prayer based on attack type (works for first attack and resync)
        if (isMageAttack && !prayingMagic) {
            prayingMagic = true;
            attackCount = 1;
            log.info("Detected mage projectile - syncing to MAGIC prayer");
        } else if (isRangeAttack && prayingMagic) {
            prayingMagic = false;
            attackCount = 1;
            log.info("Detected range projectile - syncing to RANGED prayer");
        }

        log.debug("Hunllef {} attack #{} - praying {}",
            isMageAttack ? "MAGE" : "RANGE",
            attackCount,
            prayingMagic ? "MAGIC" : "RANGED");

        if (attackCount >= ATTACKS_PER_SWITCH) {
            attackCount = 0;
            prayingMagic = !prayingMagic;
            log.info("Hunllef switching style after 4 attacks - now pray {}", prayingMagic ? "MAGIC" : "RANGED");
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!running) {
            return;
        }

        // Update hazard positions
        updateTornadoPositions();
        updateDangerousFloorTiles();

        // Handle prayers - only when in boss room and actively fighting
        boolean shouldPray = hunllef != null
            && !hunllef.isDead()
            && isInHunllefRoom()
            && (inFight || isHunllefTargetingPlayer());

        if (!shouldPray) {
            if (prayerFlickHandler != null && prayerFlickHandler.isFlicking()) {
                prayerFlickHandler.stopFlicking();
                prayerFlickHandler.deactivateProtectionPrayers();
            }
            return;
        }

        Prayer prayerThisTick = prayingMagic ? Prayer.PROTECT_FROM_MAGIC : Prayer.PROTECT_FROM_MISSILES;

        if (config.oneTickPrayer() && prayerFlickHandler != null) {
            if (prayerFlickHandler.getActivePrayer() != prayerThisTick) {
                prayerFlickHandler.switchPrayer(prayerThisTick);
            }

            if (!prayerFlickHandler.isEnabled()) {
                prayerFlickHandler.startFlicking(prayerThisTick);
            }

            prayerFlickHandler.flick();
        } else if (config.autoPrayer()) {
            if (prayingMagic) {
                prayerFlickHandler.deactivatePrayer(Prayer.PROTECT_FROM_MISSILES);
                prayerFlickHandler.activatePrayer(Prayer.PROTECT_FROM_MAGIC);
            } else {
                prayerFlickHandler.deactivatePrayer(Prayer.PROTECT_FROM_MAGIC);
                prayerFlickHandler.activatePrayer(Prayer.PROTECT_FROM_MISSILES);
            }
        }
    }

    /**
     * Update tornado positions from active tornado NPCs.
     */
    private void updateTornadoPositions() {
        tornadoTiles.clear();

        for (NPC npc : client.getNpcs()) {
            if (npc != null && isTornado(npc.getId())) {
                WorldPoint wp = npc.getWorldLocation();
                if (wp != null) {
                    tornadoTiles.add(wp);
                }
            }
        }
    }

    /**
     * Update dangerous floor tile positions from graphics objects.
     */
    private void updateDangerousFloorTiles() {
        dangerousFloorTiles.clear();

        for (GraphicsObject go : client.getGraphicsObjects()) {
            if (FLOOR_GRAPHICS.contains(go.getId())) {
                LocalPoint lp = go.getLocation();
                if (lp != null) {
                    WorldPoint wp = WorldPoint.fromLocal(client, lp);
                    dangerousFloorTiles.add(wp);
                }
            }
        }
    }

    /**
     * Get the current attack count for overlay display.
     */
    public int getAttackCount() {
        return attackCount;
    }

    /**
     * Check if we're currently praying magic.
     */
    public boolean isPrayingMagic() {
        return prayingMagic;
    }

    /**
     * Get attacks remaining until style switch.
     */
    public int getAttacksUntilSwitch() {
        return ATTACKS_PER_SWITCH - attackCount;
    }

    @Provides
    OttoGauntletConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(OttoGauntletConfig.class);
    }
}
