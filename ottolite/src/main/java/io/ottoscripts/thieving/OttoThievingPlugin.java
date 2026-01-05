package io.ottoscripts.thieving;

import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Provides;
import io.ottoscripts.util.ActionScheduler;
import io.ottoscripts.util.ClickIndicatorOverlay;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.runelite.api.Perspective;

@Slf4j
@PluginDescriptor(
    name = "OttoThieving",
    description = "Thieving helper plugin",
    tags = {"thieving", "otto", "ottolite"},
    enabledByDefault = true,
    hidden = true
)
public class OttoThievingPlugin extends Plugin {
    // Coin pouch item IDs (there are multiple variants)
    private static final Set<Integer> COIN_POUCH_IDS = new HashSet<>(Arrays.asList(
        22521, 22522, 22523, 22524, 22525, 22526, 22527, 22528, 22529, 22530, 22531, 22532, 22533, 22534, 22535, 22536, 22537, 22538
    ));

    // Shadow Veil spell widget info
    private static final int SPELLBOOK_GROUP_ID = 218;
    private static final int SHADOW_VEIL_CHILD_ID = 180;

    // Shadow Veil varbit to check if active
    private static final int SHADOW_VEIL_VARBIT = 12414;

    @Inject
    private Client client;

    @Inject
    private OttoThievingConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private SpriteManager spriteManager;

    @Getter
    private boolean running = false;

    private ActionScheduler scheduler;
    private ClickIndicatorOverlay clickOverlay;

    // State tracking
    private NPC lastPickpocketedNpc = null;

    @Override
    protected void startUp() throws Exception {
        log.info("OttoThieving started");
        clickOverlay = new ClickIndicatorOverlay(client, spriteManager);
        scheduler = new ActionScheduler();
        scheduler.setOnActionCallback(clickOverlay::showClick);
        overlayManager.add(clickOverlay);
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("OttoThieving stopped");
        running = false;
        resetState();
        if (clickOverlay != null) {
            overlayManager.remove(clickOverlay);
            clickOverlay = null;
        }
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    private void resetState() {
        lastPickpocketedNpc = null;
    }

    /**
     * Get the canvas center point of an NPC for click indicator.
     */
    private Point getNpcCanvasCenter(NPC npc) {
        if (npc == null) {
            return null;
        }
        LocalPoint lp = npc.getLocalLocation();
        if (lp == null) {
            return null;
        }
        net.runelite.api.Point canvasPoint = Perspective.localToCanvas(client, lp, client.getPlane(), npc.getLogicalHeight() / 2);
        if (canvasPoint == null) {
            return null;
        }
        return new Point(canvasPoint.getX(), canvasPoint.getY());
    }

    /**
     * Get the canvas center point of a widget for click indicator.
     */
    private Point getWidgetCanvasCenter(Widget widget) {
        if (widget == null) {
            return null;
        }
        Rectangle bounds = widget.getBounds();
        if (bounds == null) {
            return null;
        }
        return new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
    }

    public void setRunning(boolean running) {
        this.running = running;
        if (running) {
            log.info("OttoThieving enabled");
        } else {
            log.info("OttoThieving disabled");
            resetState();
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (!running || !config.pickpocketUntilFailure()) {
            return;
        }

        String message = event.getMessage().toLowerCase();

        // Detect pickpocket failure (stunned) - stop auto pickpocketing
        if (message.contains("stunned") ||
            message.contains("fail to pick") ||
            message.contains("been hit by") ||
            message.contains("you fail")) {
            lastPickpocketedNpc = null; // Stop auto pickpocketing
            log.info("Detected pickpocket failure - stopping auto pickpocket");
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (!running || !config.pickpocketUntilFailure()) {
            return;
        }

        // Only track when the action is specifically "Pickpocket"
        String option = event.getMenuOption();
        if (option != null && option.equalsIgnoreCase("Pickpocket")) {
            MenuAction action = event.getMenuAction();
            if (action == MenuAction.NPC_THIRD_OPTION || action == MenuAction.NPC_SECOND_OPTION) {
                int npcIndex = event.getId();
                for (NPC npc : client.getNpcs()) {
                    if (npc != null && npc.getIndex() == npcIndex) {
                        lastPickpocketedNpc = npc;
                        log.debug("Tracking pickpocket target: {}", npc.getName());
                        break;
                    }
                }
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!running) {
            return;
        }

        // Check for stun animation as backup detection (animation 424, 422, etc.)
        Player player = client.getLocalPlayer();
        if (player != null && config.pickpocketUntilFailure() && lastPickpocketedNpc != null) {
            int animation = player.getAnimation();
            // Stun animations from pickpocket failure
            if (animation == 424 || animation == 422 || animation == 836) {
                lastPickpocketedNpc = null; // Stop auto pickpocketing
                log.info("Detected stun via animation {} - stopping auto pickpocket", animation);
            }
        }

        if (handleAutoEat()) {
            return;
        }

        if (handleShadowVeil()) {
            return;
        }

        if (handleAutoOpenPouches()) {
            return;
        }

        if (handlePickpocket()) {
            return;
        }
    }

    private boolean handleAutoEat() {
        if (!config.autoEat()) {
            return false;
        }

        Player player = client.getLocalPlayer();
        if (player == null) {
            return false;
        }

        int currentHealth = client.getBoostedSkillLevel(Skill.HITPOINTS);
        int maxHealth = client.getRealSkillLevel(Skill.HITPOINTS);

        if (maxHealth == 0) {
            return false;
        }

        int healthPercent = (currentHealth * 100) / maxHealth;
        int threshold = config.eatHealthPercent();

        if (healthPercent <= threshold) {
            // Find food in inventory (items with "Eat" action)
            Optional<Widget> food = Inventory.search()
                .filter(item -> {
                    String[] actions = item.getActions();
                    if (actions == null) return false;
                    for (String action : actions) {
                        if (action != null && action.equalsIgnoreCase("Eat")) {
                            return true;
                        }
                    }
                    return false;
                })
                .first();

            if (food.isPresent()) {
                Widget foodWidget = food.get();
                Point foodCenter = getWidgetCanvasCenter(foodWidget);
                log.info("Auto eating - health at {}%", healthPercent);
                scheduler.schedule(() -> InventoryInteraction.useItem(foodWidget, "Eat"), foodCenter);
                return true;
            } else {
                log.debug("No food found in inventory");
            }
        }

        return false;
    }

    private boolean handleShadowVeil() {
        if (!config.autoShadowVeil()) {
            return false;
        }

        // Check if Shadow Veil is already active
        int shadowVeilActive = client.getVarbitValue(SHADOW_VEIL_VARBIT);
        if (shadowVeilActive > 0) {
            return false;
        }

        // Cast Shadow Veil spell
        Widget spellWidget = client.getWidget(SPELLBOOK_GROUP_ID, SHADOW_VEIL_CHILD_ID);
        if (spellWidget != null && !spellWidget.isHidden()) {
            Point spellCenter = getWidgetCanvasCenter(spellWidget);
            log.info("Casting Shadow Veil");
            scheduler.schedule(() -> {
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetActionPacket(1, spellWidget.getId(), -1, -1);
            }, spellCenter);
            return true;
        } else {
            log.debug("Shadow Veil spell not available (wrong spellbook or hidden)");
        }

        return false;
    }

    private boolean handleAutoOpenPouches() {
        if (!config.autoOpenPouches()) {
            return false;
        }

        // Count coin pouches in inventory
        int pouchCount = 0;
        for (int pouchId : COIN_POUCH_IDS) {
            pouchCount += Inventory.getItemAmount(pouchId);
        }

        if (pouchCount >= config.pouchThreshold()) {
            // Open a coin pouch
            Optional<Widget> pouch = Inventory.search()
                .filter(item -> COIN_POUCH_IDS.contains(item.getItemId()))
                .first();

            if (pouch.isPresent()) {
                Widget pouchWidget = pouch.get();
                Point pouchCenter = getWidgetCanvasCenter(pouchWidget);
                log.info("Opening coin pouches - count: {}", pouchCount);
                scheduler.schedule(() -> InventoryInteraction.useItem(pouchWidget, "Open-all"), pouchCenter);
                return true;
            }
        }

        return false;
    }

    private boolean handlePickpocket() {
        if (!config.pickpocketUntilFailure()) {
            return false;
        }

        // Try to pickpocket the last targeted NPC (spam-click style)
        if (lastPickpocketedNpc != null && !lastPickpocketedNpc.isDead()) {
            // Verify NPC is still nearby
            Optional<NPC> targetNpc = NPCs.search()
                .indexIs(lastPickpocketedNpc.getIndex())
                .first();

            if (targetNpc.isPresent()) {
                NPC npc = targetNpc.get();
                Point npcCenter = getNpcCanvasCenter(npc);
                // Spam pickpocket with random 150-200ms delays between clicks
                scheduler.schedule(() -> NPCInteraction.interact(npc, "Pickpocket"), 150, 200, npcCenter);
                return true;
            } else {
                // NPC no longer nearby, clear target
                lastPickpocketedNpc = null;
            }
        }

        return false;
    }

    @Provides
    OttoThievingConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(OttoThievingConfig.class);
    }
}
