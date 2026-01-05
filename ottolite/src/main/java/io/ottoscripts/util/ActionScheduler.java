package io.ottoscripts.util;

import lombok.extern.slf4j.Slf4j;

import java.awt.Point;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Utility for scheduling actions with randomized delays to reduce detection.
 */
@Slf4j
public class ActionScheduler {
    private static final int DEFAULT_MIN_DELAY_MS = 1;
    private static final int DEFAULT_MAX_DELAY_MS = 50;

    private final Random random = new Random();
    private final ScheduledExecutorService executor;
    private final int minDelayMs;
    private final int maxDelayMs;

    private Consumer<Point> onActionCallback;

    public ActionScheduler() {
        this(DEFAULT_MIN_DELAY_MS, DEFAULT_MAX_DELAY_MS);
    }

    public ActionScheduler(int minDelayMs, int maxDelayMs) {
        this.minDelayMs = minDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Set a callback that runs whenever an action is scheduled.
     * Useful for click indicators or other visual feedback.
     * The callback receives the target Point (entity location) or null for mouse position.
     *
     * @param callback the callback to run, receives target Point
     */
    public void setOnActionCallback(Consumer<Point> callback) {
        this.onActionCallback = callback;
    }

    /**
     * Schedule an action with a random delay within the configured range.
     *
     * @param action the action to execute
     * @return the scheduled future, or null if executor is shut down
     */
    public ScheduledFuture<?> schedule(Runnable action) {
        return schedule(action, null);
    }

    /**
     * Schedule an action with a random delay and target location for click indicator.
     *
     * @param action the action to execute
     * @param targetLocation the screen location of the target entity (for click indicator)
     * @return the scheduled future, or null if executor is shut down
     */
    public ScheduledFuture<?> schedule(Runnable action, Point targetLocation) {
        int delay = randomDelay();
        return schedule(action, delay, targetLocation);
    }

    /**
     * Schedule an action with a specific delay.
     *
     * @param action the action to execute
     * @param delayMs the delay in milliseconds
     * @return the scheduled future, or null if executor is shut down
     */
    public ScheduledFuture<?> schedule(Runnable action, int delayMs) {
        return schedule(action, delayMs, null);
    }

    /**
     * Schedule an action with a specific delay and target location.
     *
     * @param action the action to execute
     * @param delayMs the delay in milliseconds
     * @param targetLocation the screen location of the target entity (for click indicator)
     * @return the scheduled future, or null if executor is shut down
     */
    public ScheduledFuture<?> schedule(Runnable action, int delayMs, Point targetLocation) {
        if (executor.isShutdown()) {
            log.warn("Cannot schedule action - executor is shut down");
            return null;
        }

        // Trigger callback (e.g., click indicator) with target location
        if (onActionCallback != null) {
            try {
                onActionCallback.accept(targetLocation);
            } catch (Exception e) {
                log.debug("Error in action callback", e);
            }
        }

        log.debug("Scheduling action with {}ms delay", delayMs);
        return executor.schedule(action, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Schedule an action with a custom random delay range.
     *
     * @param action the action to execute
     * @param minMs minimum delay in milliseconds
     * @param maxMs maximum delay in milliseconds
     * @return the scheduled future, or null if executor is shut down
     */
    public ScheduledFuture<?> schedule(Runnable action, int minMs, int maxMs) {
        return schedule(action, minMs, maxMs, null);
    }

    /**
     * Schedule an action with a custom random delay range and target location.
     *
     * @param action the action to execute
     * @param minMs minimum delay in milliseconds
     * @param maxMs maximum delay in milliseconds
     * @param targetLocation the screen location of the target entity (for click indicator)
     * @return the scheduled future, or null if executor is shut down
     */
    public ScheduledFuture<?> schedule(Runnable action, int minMs, int maxMs, Point targetLocation) {
        int delay = minMs + random.nextInt(maxMs - minMs + 1);
        return schedule(action, delay, targetLocation);
    }

    /**
     * Generate a random delay within the configured range.
     *
     * @return random delay in milliseconds
     */
    public int randomDelay() {
        return minDelayMs + random.nextInt(maxDelayMs - minDelayMs + 1);
    }

    /**
     * Generate a random delay within a custom range.
     *
     * @param minMs minimum delay
     * @param maxMs maximum delay
     * @return random delay in milliseconds
     */
    public int randomDelay(int minMs, int maxMs) {
        return minMs + random.nextInt(maxMs - minMs + 1);
    }

    /**
     * Get a random int within a range (inclusive).
     *
     * @param min minimum value
     * @param max maximum value
     * @return random int
     */
    public int randomInt(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    /**
     * Shutdown the executor. Should be called when the plugin stops.
     */
    public void shutdown() {
        if (!executor.isShutdown()) {
            executor.shutdownNow();
            log.debug("ActionScheduler shut down");
        }
    }

    /**
     * Check if the executor is shut down.
     *
     * @return true if shut down
     */
    public boolean isShutdown() {
        return executor.isShutdown();
    }
}
