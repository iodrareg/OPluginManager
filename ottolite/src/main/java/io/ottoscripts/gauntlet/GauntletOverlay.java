package io.ottoscripts.gauntlet;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;

public class GauntletOverlay extends Overlay {
    private static final Color MAGIC_COLOR = new Color(0, 150, 255);
    private static final Color RANGED_COLOR = new Color(0, 255, 0);

    private final Client client;
    private final OttoGauntletPlugin plugin;
    private final PanelComponent panelComponent = new PanelComponent();

    public GauntletOverlay(Client client, OttoGauntletPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!plugin.isRunning()) {
            return null;
        }

        panelComponent.getChildren().clear();

        // Title
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("Hunllef")
            .color(Color.WHITE)
            .build());

        // Current prayer style
        String prayerStyle = plugin.isPrayingMagic() ? "MAGIC" : "RANGED";
        Color prayerColor = plugin.isPrayingMagic() ? MAGIC_COLOR : RANGED_COLOR;

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Pray:")
            .right(prayerStyle)
            .rightColor(prayerColor)
            .build());

        // Attack counter
        int attacksUntilSwitch = plugin.getAttacksUntilSwitch();
        panelComponent.getChildren().add(LineComponent.builder()
            .left("Switch in:")
            .right(String.valueOf(attacksUntilSwitch))
            .rightColor(attacksUntilSwitch == 1 ? Color.RED : Color.YELLOW)
            .build());

        return panelComponent.render(graphics);
    }
}
