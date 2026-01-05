package io.ottoscripts.gauntlet;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import java.awt.*;

public class GauntletTileOverlay extends Overlay {
    private static final Color TORNADO_TILE_COLOR = new Color(255, 165, 0, 80);
    private static final Color TORNADO_TILE_BORDER = new Color(255, 165, 0, 180);
    private static final Color FLOOR_TILE_COLOR = new Color(255, 0, 0, 80);
    private static final Color FLOOR_TILE_BORDER = new Color(255, 0, 0, 180);

    private final Client client;
    private final OttoGauntletPlugin plugin;

    public GauntletTileOverlay(Client client, OttoGauntletPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!plugin.isRunning()) {
            return null;
        }

        // Render tornado tiles (orange)
        for (WorldPoint tile : plugin.getTornadoTiles()) {
            renderTile(graphics, tile, TORNADO_TILE_COLOR, TORNADO_TILE_BORDER);
        }

        // Render dangerous floor tiles (red)
        for (WorldPoint tile : plugin.getDangerousFloorTiles()) {
            renderTile(graphics, tile, FLOOR_TILE_COLOR, FLOOR_TILE_BORDER);
        }

        return null;
    }

    private void renderTile(Graphics2D graphics, WorldPoint worldPoint, Color fillColor, Color borderColor) {
        if (worldPoint.getPlane() != client.getPlane()) {
            return;
        }

        LocalPoint lp = LocalPoint.fromWorld(client, worldPoint);
        if (lp == null) {
            return;
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly == null) {
            return;
        }

        // Fill
        graphics.setColor(fillColor);
        graphics.fill(poly);

        // Border
        graphics.setColor(borderColor);
        graphics.setStroke(new BasicStroke(2));
        graphics.draw(poly);
    }
}
