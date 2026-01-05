package io.ottoscripts.agility;

import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.InteractionApi.TileObjectInteraction;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import io.ottoscripts.util.ActionScheduler;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@PluginDescriptor(
    name = "OttoAgility",
    description = "Agility helper plugin",
    tags = {"agility", "otto", "ottolite"},
    enabledByDefault = true,
    hidden = true
)
public class OttoAgilityPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private OttoAgilityConfig config;

    @Getter
    private boolean running = false;

    private ActionScheduler scheduler;

    // Current course state
    private List<Integer> currentCourse = null;
    private int currentObstacleIndex = -1;
    private int ticksIdle = 0;
    private LocalPoint lastPlayerLocation = null;
    private int stepsAutomated = 0;

    // Obstacle course definitions - each list contains object IDs in order
    public static final List<Integer> GNOME_STRONGHOLD = Arrays.asList(
        23145,  // Log balance
        23134,  // Obstacle net
        23559,  // Tree branch
        23557,  // Balancing rope
        23560,  // Tree branch down
        23135,  // Obstacle net
        23138   // Obstacle pipe
    );

    public static final List<Integer> DRAYNOR_VILLAGE = Arrays.asList(
        11404,  // Rough wall
        11405,  // Tightrope
        11406,  // Tightrope
        11430,  // Narrow wall
        11630,  // Wall
        11631,  // Gap
        11632   // Crate
    );

    public static final List<Integer> AL_KHARID = Arrays.asList(
        11633,  // Rough wall
        14398,  // Tightrope
        14402,  // Cable
        14403,  // Zip line
        14404,  // Tropical tree
        14405,  // Roof top beams
        14406   // Tightrope
    );

    public static final List<Integer> VARROCK = Arrays.asList(
        14412,  // Rough wall
        14413,  // Clothes line
        14414,  // Gap
        14832,  // Wall
        14833,  // Gap
        14834,  // Gap
        14835,  // Gap
        14836   // Edge
    );

    public static final List<Integer> CANIFIS = Arrays.asList(
        14843,  // Tall tree
        14844,  // Gap
        14845,  // Gap
        14848,  // Gap
        14846,  // Pole-vault
        14894,  // Gap
        14847   // Gap
    );

    public static final List<Integer> FALADOR = Arrays.asList(
        14898,  // Rough wall
        14899,  // Tightrope
        14901,  // Hand holds
        14903,  // Gap
        14904,  // Gap
        14905,  // Tightrope
        14911,  // Tightrope
        14919,  // Gap
        14920,  // Ledge
        14921,  // Ledge
        14922,  // Ledge
        14924,  // Ledge
        14925   // Edge
    );

    public static final List<Integer> SEERS_VILLAGE = Arrays.asList(
        14927,  // Wall
        14928,  // Gap
        14932,  // Tightrope
        14929,  // Gap
        14930,  // Gap
        14931   // Edge
    );

    public static final List<Integer> POLLNIVNEACH = Arrays.asList(
        14935,  // Basket
        14936,  // Market stall
        14937,  // Banner
        14938,  // Gap
        14939,  // Tree
        14940,  // Rough wall
        14941,  // Monkeybars
        14944,  // Tree
        14945   // Drying line
    );

    public static final List<Integer> RELLEKKA = Arrays.asList(
        14946,  // Rough wall
        14947,  // Gap
        14987,  // Tightrope
        14990,  // Gap
        14991,  // Gap
        14992,  // Tightrope
        14994   // Pile of fish
    );

    public static final List<Integer> ARDOUGNE = Arrays.asList(
        15608,  // Wooden beams
        15609,  // Gap
        26635,  // Plank
        15610,  // Gap
        15611,  // Gap
        28912,  // Steep roof
        15612   // Gap
    );

    // Map of all courses for quick lookup
    private static final List<List<Integer>> ALL_COURSES = Arrays.asList(
        GNOME_STRONGHOLD,
        DRAYNOR_VILLAGE,
        AL_KHARID,
        VARROCK,
        CANIFIS,
        FALADOR,
        SEERS_VILLAGE,
        POLLNIVNEACH,
        RELLEKKA,
        ARDOUGNE
    );

    // Map object ID to its course and index
    private final Map<Integer, CourseObstacle> obstacleMap = new HashMap<>();

    private static class CourseObstacle {
        final List<Integer> course;
        final int index;

        CourseObstacle(List<Integer> course, int index) {
            this.course = course;
            this.index = index;
        }
    }

    @Override
    protected void startUp() throws Exception {
        log.info("OttoAgility started");
        buildObstacleMap();
        scheduler = new ActionScheduler();
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("OttoAgility stopped");
        running = false;
        resetCourseState();
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    private void buildObstacleMap() {
        obstacleMap.clear();
        for (List<Integer> course : ALL_COURSES) {
            for (int i = 0; i < course.size(); i++) {
                obstacleMap.put(course.get(i), new CourseObstacle(course, i));
            }
        }
    }

    private void resetCourseState() {
        currentCourse = null;
        currentObstacleIndex = -1;
        ticksIdle = 0;
        lastPlayerLocation = null;
        stepsAutomated = 0;
    }

    public void setRunning(boolean running) {
        this.running = running;
        if (running) {
            log.info("OttoAgility enabled");
        } else {
            log.info("OttoAgility disabled");
            resetCourseState();
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (!running) {
            return;
        }

        int objectId = event.getId();

        // Check if clicked object is an obstacle in any course
        CourseObstacle obstacle = obstacleMap.get(objectId);
        if (obstacle != null) {
            currentCourse = obstacle.course;
            currentObstacleIndex = obstacle.index;
            ticksIdle = 0;
            log.info("Started course at obstacle index {}", currentObstacleIndex);
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!running || currentCourse == null) {
            return;
        }

        Player player = client.getLocalPlayer();
        if (player == null) {
            return;
        }

        // Check if player is idle (not moving and not animating)
        boolean isAnimating = player.getAnimation() != -1;
        boolean isMoving = false;

        LocalPoint currentLocation = player.getLocalLocation();
        if (lastPlayerLocation != null) {
            isMoving = !currentLocation.equals(lastPlayerLocation);
        }
        lastPlayerLocation = currentLocation;

        if (isAnimating || isMoving) {
            ticksIdle = 0;
            return;
        }

        ticksIdle++;

        // Wait a tick to ensure player is truly idle
        if (ticksIdle < 2) {
            return;
        }

        // Find and click next obstacle
        int nextIndex = (currentObstacleIndex + 1) % currentCourse.size();
        int nextObstacleId = currentCourse.get(nextIndex);

        Optional<TileObject> nextObstacle = TileObjects.search()
            .withId(nextObstacleId)
            .nearestToPlayer();

        if (nextObstacle.isPresent()) {
            TileObject obstacle = nextObstacle.get();
            stepsAutomated++;

            int maxSteps = config.stepsToAutomate();
            log.info("Clicking obstacle: {} (index {}), step {}/{}, coords: ({}, {}, {})",
                nextObstacleId, nextIndex, stepsAutomated, maxSteps,
                obstacle.getWorldLocation().getX(),
                obstacle.getWorldLocation().getY(),
                obstacle.getWorldLocation().getPlane());

            // Schedule interaction with random delay
            scheduler.schedule(() -> TileObjectInteraction.interact(obstacle, 1));

            // Update to next obstacle index
            currentObstacleIndex = nextIndex;
            ticksIdle = 0;

            // Stop if we've automated enough steps
            if (stepsAutomated >= maxSteps) {
                log.info("Completed {} automated steps, stopping", stepsAutomated);
                resetCourseState();
            }
        } else {
            log.debug("Next obstacle {} not found nearby", nextObstacleId);
        }
    }

    @Provides
    OttoAgilityConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(OttoAgilityConfig.class);
    }
}
