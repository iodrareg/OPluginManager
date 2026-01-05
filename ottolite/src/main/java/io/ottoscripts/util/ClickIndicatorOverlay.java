package io.ottoscripts.util;

import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ClickIndicatorOverlay extends Overlay {
    // Yellow click animation sprite IDs (frames 1-4)
    private static final int[] YELLOW_CLICK_SPRITES = {
        SpriteID.YELLOW_CLICK_ANIMATION_1,
        SpriteID.YELLOW_CLICK_ANIMATION_2,
        SpriteID.YELLOW_CLICK_ANIMATION_3,
        SpriteID.YELLOW_CLICK_ANIMATION_4
    };

    private static final int FRAME_DURATION_MS = 100; // Duration per frame
    private static final int TOTAL_DURATION_MS = FRAME_DURATION_MS * YELLOW_CLICK_SPRITES.length;
    private static final Color CYAN_TINT = new Color(0, 255, 255); // Cyan color

    private final Client client;
    private final SpriteManager spriteManager;
    private final BufferedImage[] tintedSprites = new BufferedImage[YELLOW_CLICK_SPRITES.length];

    private long clickStartTime = 0;
    private Point clickLocation = null;

    public ClickIndicatorOverlay(Client client, SpriteManager spriteManager) {
        this.client = client;
        this.spriteManager = spriteManager;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }

    private BufferedImage getTintedSprite(int frameIndex) {
        // Cache tinted sprites
        if (tintedSprites[frameIndex] != null) {
            return tintedSprites[frameIndex];
        }

        BufferedImage original = spriteManager.getSprite(YELLOW_CLICK_SPRITES[frameIndex], 0);
        if (original == null) {
            return null;
        }

        // Create a copy and tint it cyan
        BufferedImage tinted = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                int argb = original.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;

                if (alpha > 0) {
                    // Replace color with cyan, keeping the alpha
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;

                    // Use brightness of original pixel to modulate cyan
                    float brightness = (r + g + b) / (3f * 255f);

                    int newR = (int) (CYAN_TINT.getRed() * brightness);
                    int newG = (int) (CYAN_TINT.getGreen() * brightness);
                    int newB = (int) (CYAN_TINT.getBlue() * brightness);

                    tinted.setRGB(x, y, (alpha << 24) | (newR << 16) | (newG << 8) | newB);
                } else {
                    tinted.setRGB(x, y, 0);
                }
            }
        }

        tintedSprites[frameIndex] = tinted;
        return tinted;
    }

    /**
     * Show click animation at the specified canvas location.
     * @param location the point on the canvas to show the click (can be null to use mouse position)
     */
    public void showClick(Point location) {
        try {
            if (location != null) {
                clickLocation = location;
            } else {
                // Fallback to mouse position
                clickLocation = MouseInfo.getPointerInfo().getLocation();
                Point canvasLocation = client.getCanvas().getLocationOnScreen();
                clickLocation = new Point(
                    clickLocation.x - canvasLocation.x,
                    clickLocation.y - canvasLocation.y
                );
            }
            clickStartTime = System.currentTimeMillis();
        } catch (Exception e) {
            // Canvas might not be visible
        }
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (clickLocation == null) {
            return null;
        }

        long elapsed = System.currentTimeMillis() - clickStartTime;
        if (elapsed > TOTAL_DURATION_MS) {
            clickLocation = null;
            return null;
        }

        // Calculate current frame
        int frameIndex = (int) (elapsed / FRAME_DURATION_MS);
        if (frameIndex >= YELLOW_CLICK_SPRITES.length) {
            frameIndex = YELLOW_CLICK_SPRITES.length - 1;
        }

        // Get the tinted sprite for current frame
        BufferedImage sprite = getTintedSprite(frameIndex);
        if (sprite != null) {
            // Draw sprite centered on click location
            int x = clickLocation.x - sprite.getWidth() / 2;
            int y = clickLocation.y - sprite.getHeight() / 2;
            graphics.drawImage(sprite, x, y, null);
        }

        return null;
    }
}
