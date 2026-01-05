package io.ottoscripts.scurrius;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.*;

public class ScurriusTileOverlay extends Overlay {
    private static final Color DANGER_COLOR = new Color(255, 0, 0, 150);
    private static final Color DANGER_BORDER_COLOR = new Color(255, 0, 0, 255);

    private final Client client;
    private final OttoScurriusPlugin plugin;

    public ScurriusTileOverlay(Client client, OttoScurriusPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!plugin.isRunning()) {
            return null;
        }

        for (WorldPoint worldPoint : plugin.getDangerousTiles()) {
            renderTile(graphics, worldPoint);
        }

        return null;
    }

    private void renderTile(Graphics2D graphics, WorldPoint worldPoint) {
        if (worldPoint.getPlane() != client.getPlane()) {
            return;
        }

        LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
        if (localPoint == null) {
            return;
        }

        Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
        if (polygon == null) {
            return;
        }

        // Fill the tile
        graphics.setColor(DANGER_COLOR);
        graphics.fill(polygon);

        // Draw border
        graphics.setColor(DANGER_BORDER_COLOR);
        graphics.setStroke(new BasicStroke(2));
        graphics.draw(polygon);
    }
}
