package io.ottoscripts;

import com.google.inject.Provides;
import io.ottoscripts.agility.OttoAgilityConfig;
import io.ottoscripts.agility.OttoAgilityPlugin;
import io.ottoscripts.gauntlet.OttoGauntletConfig;
import io.ottoscripts.gauntlet.OttoGauntletPlugin;
import io.ottoscripts.scurrius.OttoScurriusConfig;
import io.ottoscripts.scurrius.OttoScurriusPlugin;
import io.ottoscripts.thieving.OttoThievingConfig;
import io.ottoscripts.thieving.OttoThievingPlugin;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
    name = "OttoLite",
    description = "OttoLite plugin suite with feature toggles",
    tags = {"ottolite", "otto", "utility"}
)
public class OttoLitePlugin extends Plugin {
    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private OttoLiteConfig config;

    @Inject
    private ConfigManager configManager;

    private OttoLitePanel panel;
    private NavigationButton navButton;
    private OttoAgilityPlugin agilityPlugin;
    private OttoScurriusPlugin scurriusPlugin;
    private OttoThievingPlugin thievingPlugin;
    private OttoGauntletPlugin gauntletPlugin;

    @Override
    protected void startUp() throws Exception {
        log.info("OttoLite started");

        // Find plugin instances
        for (Plugin plugin : pluginManager.getPlugins()) {
            if (plugin instanceof OttoAgilityPlugin) {
                agilityPlugin = (OttoAgilityPlugin) plugin;
            } else if (plugin instanceof OttoScurriusPlugin) {
                scurriusPlugin = (OttoScurriusPlugin) plugin;
            } else if (plugin instanceof OttoThievingPlugin) {
                thievingPlugin = (OttoThievingPlugin) plugin;
            } else if (plugin instanceof OttoGauntletPlugin) {
                gauntletPlugin = (OttoGauntletPlugin) plugin;
            }
        }

        // Get enabled state from config and apply it
        OttoAgilityConfig agilityConfig = configManager.getConfig(OttoAgilityConfig.class);
        boolean agilityEnabled = agilityConfig.enabled();
        if (agilityPlugin != null && agilityEnabled) {
            agilityPlugin.setRunning(true);
        }

        panel = new OttoLitePanel();

        // ========== SKILLING ==========
        panel.addCategoryHeader("Skilling");

        // Agility Helper
        JPanel agilitySettingsPanel = createAgilitySettingsPanel();
        panel.addFeatureToggle(
            "agility",
            "Agility Helper",
            agilityEnabled,
            this::toggleAgility,
            agilitySettingsPanel
        );

        // Thieving Helper
        OttoThievingConfig thievingConfig = configManager.getConfig(OttoThievingConfig.class);
        boolean thievingEnabled = thievingConfig.enabled();
        if (thievingPlugin != null && thievingEnabled) {
            thievingPlugin.setRunning(true);
        }
        JPanel thievingSettingsPanel = createThievingSettingsPanel();
        panel.addFeatureToggle(
            "thieving",
            "Thieving Helper",
            thievingEnabled,
            this::toggleThieving,
            thievingSettingsPanel
        );

        // ========== PVM ==========
        panel.addCategoryHeader("PvM");

        // Scurrius Helper
        OttoScurriusConfig scurriusConfig = configManager.getConfig(OttoScurriusConfig.class);
        boolean scurriusEnabled = scurriusConfig.enabled();
        if (scurriusPlugin != null && scurriusEnabled) {
            scurriusPlugin.setRunning(true);
        }
        JPanel scurriusSettingsPanel = createScurriusSettingsPanel();
        panel.addFeatureToggle(
            "scurrius",
            "Scurrius Helper",
            scurriusEnabled,
            this::toggleScurrius,
            scurriusSettingsPanel
        );

        // Gauntlet Helper
        OttoGauntletConfig gauntletConfig = configManager.getConfig(OttoGauntletConfig.class);
        boolean gauntletEnabled = gauntletConfig.enabled();
        if (gauntletPlugin != null && gauntletEnabled) {
            gauntletPlugin.setRunning(true);
        }
        JPanel gauntletSettingsPanel = createGauntletSettingsPanel();
        panel.addFeatureToggle(
            "gauntlet",
            "Gauntlet Helper",
            gauntletEnabled,
            this::toggleGauntlet,
            gauntletSettingsPanel
        );

        // Load the icon for the sidebar
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon_otto.png");

        navButton = NavigationButton.builder()
            .tooltip("OttoLite")
            .icon(icon != null ? icon : new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB))
            .priority(5)
            .panel(panel)
            .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("OttoLite stopped");
        clientToolbar.removeNavigation(navButton);
        panel = null;
        navButton = null;
    }

    private void toggleAgility(boolean enabled) {
        // Save to config
        configManager.setConfiguration("ottoagility", "enabled", enabled);

        if (agilityPlugin != null) {
            agilityPlugin.setRunning(enabled);
            log.info("OttoAgility {}", enabled ? "enabled" : "disabled");
        } else {
            log.warn("OttoAgility plugin not found");
        }
    }

    private void toggleScurrius(boolean enabled) {
        // Save to config
        configManager.setConfiguration("ottoscurrius", "enabled", enabled);

        if (scurriusPlugin != null) {
            scurriusPlugin.setRunning(enabled);
            log.info("OttoScurrius {}", enabled ? "enabled" : "disabled");
        } else {
            log.warn("OttoScurrius plugin not found");
        }
    }

    private void toggleThieving(boolean enabled) {
        // Save to config
        configManager.setConfiguration("ottothieving", "enabled", enabled);

        if (thievingPlugin != null) {
            thievingPlugin.setRunning(enabled);
            log.info("OttoThieving {}", enabled ? "enabled" : "disabled");
        } else {
            log.warn("OttoThieving plugin not found");
        }
    }

    private void toggleGauntlet(boolean enabled) {
        // Save to config
        configManager.setConfiguration("ottogauntlet", "enabled", enabled);

        if (gauntletPlugin != null) {
            gauntletPlugin.setRunning(enabled);
            log.info("OttoGauntlet {}", enabled ? "enabled" : "disabled");
        } else {
            log.warn("OttoGauntlet plugin not found");
        }
    }

    private JPanel createAgilitySettingsPanel() {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Steps to Automate setting
        JPanel stepsRow = new JPanel(new BorderLayout(10, 0));
        stepsRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        stepsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        stepsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel stepsLabel = new JLabel("Steps to automate");
        stepsLabel.setForeground(Color.WHITE);

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 100, 1);
        JSpinner stepsSpinner = new JSpinner(spinnerModel);
        stepsSpinner.setPreferredSize(new Dimension(60, 24));

        // Save value when changed
        stepsSpinner.addChangeListener(e -> {
            int value = (Integer) stepsSpinner.getValue();
            configManager.setConfiguration("ottoagility", "stepsToAutomate", value);
        });

        // Update spinner value from config when panel becomes visible
        settingsPanel.addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override
            public void ancestorAdded(javax.swing.event.AncestorEvent event) {
                OttoAgilityConfig agilityConfig = configManager.getConfig(OttoAgilityConfig.class);
                stepsSpinner.setValue(agilityConfig.stepsToAutomate());
            }

            @Override
            public void ancestorRemoved(javax.swing.event.AncestorEvent event) {}

            @Override
            public void ancestorMoved(javax.swing.event.AncestorEvent event) {}
        });

        stepsRow.add(stepsLabel, BorderLayout.WEST);
        stepsRow.add(stepsSpinner, BorderLayout.EAST);

        settingsPanel.add(stepsRow);

        return settingsPanel;
    }

    private JPanel createScurriusSettingsPanel() {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Auto Prayer checkbox
        JCheckBox autoPrayerCheckbox = new JCheckBox("Auto Prayer");
        autoPrayerCheckbox.setBackground(ColorScheme.DARK_GRAY_COLOR);
        autoPrayerCheckbox.setForeground(Color.WHITE);
        autoPrayerCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoPrayerCheckbox.addActionListener(e -> {
            configManager.setConfiguration("ottoscurrius", "autoPrayer", autoPrayerCheckbox.isSelected());
        });

        // 1 Tick Prayer checkbox
        JCheckBox oneTickPrayerCheckbox = new JCheckBox("1 Tick Prayer");
        oneTickPrayerCheckbox.setBackground(ColorScheme.DARK_GRAY_COLOR);
        oneTickPrayerCheckbox.setForeground(Color.WHITE);
        oneTickPrayerCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        oneTickPrayerCheckbox.addActionListener(e -> {
            configManager.setConfiguration("ottoscurrius", "oneTickPrayer", oneTickPrayerCheckbox.isSelected());
        });

        // Update checkbox values from config when panel becomes visible
        settingsPanel.addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override
            public void ancestorAdded(javax.swing.event.AncestorEvent event) {
                OttoScurriusConfig scurriusConfig = configManager.getConfig(OttoScurriusConfig.class);
                autoPrayerCheckbox.setSelected(scurriusConfig.autoPrayer());
                oneTickPrayerCheckbox.setSelected(scurriusConfig.oneTickPrayer());
            }

            @Override
            public void ancestorRemoved(javax.swing.event.AncestorEvent event) {}

            @Override
            public void ancestorMoved(javax.swing.event.AncestorEvent event) {}
        });

        settingsPanel.add(autoPrayerCheckbox);
        settingsPanel.add(Box.createVerticalStrut(5));
        settingsPanel.add(oneTickPrayerCheckbox);

        return settingsPanel;
    }

    private JPanel createThievingSettingsPanel() {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Pickpocket until failure checkbox
        JCheckBox pickpocketCheckbox = new JCheckBox("Pickpocket until failure");
        pickpocketCheckbox.setBackground(ColorScheme.DARK_GRAY_COLOR);
        pickpocketCheckbox.setForeground(Color.WHITE);
        pickpocketCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        pickpocketCheckbox.addActionListener(e -> {
            configManager.setConfiguration("ottothieving", "pickpocketUntilFailure", pickpocketCheckbox.isSelected());
        });

        // Auto open money pouch row
        JPanel pouchRow = new JPanel(new BorderLayout(10, 0));
        pouchRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        pouchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        pouchRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox pouchCheckbox = new JCheckBox("Auto open pouch at");
        pouchCheckbox.setBackground(ColorScheme.DARK_GRAY_COLOR);
        pouchCheckbox.setForeground(Color.WHITE);

        SpinnerNumberModel pouchSpinnerModel = new SpinnerNumberModel(28, 1, 28, 1);
        JSpinner pouchSpinner = new JSpinner(pouchSpinnerModel);
        pouchSpinner.setPreferredSize(new Dimension(60, 24));
        pouchSpinner.setEnabled(false);

        pouchCheckbox.addActionListener(e -> {
            boolean selected = pouchCheckbox.isSelected();
            pouchSpinner.setEnabled(selected);
            configManager.setConfiguration("ottothieving", "autoOpenPouches", selected);
        });

        pouchSpinner.addChangeListener(e -> {
            int value = (Integer) pouchSpinner.getValue();
            configManager.setConfiguration("ottothieving", "pouchThreshold", value);
        });

        pouchRow.add(pouchCheckbox, BorderLayout.WEST);
        pouchRow.add(pouchSpinner, BorderLayout.EAST);

        // Autocast Shadow Veil checkbox
        JCheckBox shadowVeilCheckbox = new JCheckBox("Autocast Shadow Veil");
        shadowVeilCheckbox.setBackground(ColorScheme.DARK_GRAY_COLOR);
        shadowVeilCheckbox.setForeground(Color.WHITE);
        shadowVeilCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        shadowVeilCheckbox.addActionListener(e -> {
            configManager.setConfiguration("ottothieving", "autoShadowVeil", shadowVeilCheckbox.isSelected());
        });

        // Auto eat food row
        JPanel eatRow = new JPanel(new BorderLayout(10, 0));
        eatRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        eatRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        eatRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox eatCheckbox = new JCheckBox("Auto eat at HP %");
        eatCheckbox.setBackground(ColorScheme.DARK_GRAY_COLOR);
        eatCheckbox.setForeground(Color.WHITE);

        SpinnerNumberModel eatSpinnerModel = new SpinnerNumberModel(50, 1, 99, 1);
        JSpinner eatSpinner = new JSpinner(eatSpinnerModel);
        eatSpinner.setPreferredSize(new Dimension(60, 24));
        eatSpinner.setEnabled(false);

        eatCheckbox.addActionListener(e -> {
            boolean selected = eatCheckbox.isSelected();
            eatSpinner.setEnabled(selected);
            configManager.setConfiguration("ottothieving", "autoEat", selected);
        });

        eatSpinner.addChangeListener(e -> {
            int value = (Integer) eatSpinner.getValue();
            configManager.setConfiguration("ottothieving", "eatHealthPercent", value);
        });

        eatRow.add(eatCheckbox, BorderLayout.WEST);
        eatRow.add(eatSpinner, BorderLayout.EAST);

        // Update values from config when panel becomes visible
        settingsPanel.addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override
            public void ancestorAdded(javax.swing.event.AncestorEvent event) {
                OttoThievingConfig thievingConfig = configManager.getConfig(OttoThievingConfig.class);
                pickpocketCheckbox.setSelected(thievingConfig.pickpocketUntilFailure());
                pouchCheckbox.setSelected(thievingConfig.autoOpenPouches());
                pouchSpinner.setValue(thievingConfig.pouchThreshold());
                pouchSpinner.setEnabled(thievingConfig.autoOpenPouches());
                shadowVeilCheckbox.setSelected(thievingConfig.autoShadowVeil());
                eatCheckbox.setSelected(thievingConfig.autoEat());
                eatSpinner.setValue(thievingConfig.eatHealthPercent());
                eatSpinner.setEnabled(thievingConfig.autoEat());
            }

            @Override
            public void ancestorRemoved(javax.swing.event.AncestorEvent event) {}

            @Override
            public void ancestorMoved(javax.swing.event.AncestorEvent event) {}
        });

        settingsPanel.add(pickpocketCheckbox);
        settingsPanel.add(Box.createVerticalStrut(5));
        settingsPanel.add(pouchRow);
        settingsPanel.add(Box.createVerticalStrut(5));
        settingsPanel.add(shadowVeilCheckbox);
        settingsPanel.add(Box.createVerticalStrut(5));
        settingsPanel.add(eatRow);

        return settingsPanel;
    }

    private JPanel createGauntletSettingsPanel() {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Auto Prayer checkbox
        JCheckBox autoPrayerCheckbox = new JCheckBox("Auto Prayer");
        autoPrayerCheckbox.setBackground(ColorScheme.DARK_GRAY_COLOR);
        autoPrayerCheckbox.setForeground(Color.WHITE);
        autoPrayerCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoPrayerCheckbox.addActionListener(e -> {
            configManager.setConfiguration("ottogauntlet", "autoPrayer", autoPrayerCheckbox.isSelected());
        });

        // 1 Tick Prayer checkbox
        JCheckBox oneTickPrayerCheckbox = new JCheckBox("1 Tick Prayer");
        oneTickPrayerCheckbox.setBackground(ColorScheme.DARK_GRAY_COLOR);
        oneTickPrayerCheckbox.setForeground(Color.WHITE);
        oneTickPrayerCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        oneTickPrayerCheckbox.addActionListener(e -> {
            configManager.setConfiguration("ottogauntlet", "oneTickPrayer", oneTickPrayerCheckbox.isSelected());
        });

        // Update checkbox values from config when panel becomes visible
        settingsPanel.addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override
            public void ancestorAdded(javax.swing.event.AncestorEvent event) {
                OttoGauntletConfig gauntletConfig = configManager.getConfig(OttoGauntletConfig.class);
                autoPrayerCheckbox.setSelected(gauntletConfig.autoPrayer());
                oneTickPrayerCheckbox.setSelected(gauntletConfig.oneTickPrayer());
            }

            @Override
            public void ancestorRemoved(javax.swing.event.AncestorEvent event) {}

            @Override
            public void ancestorMoved(javax.swing.event.AncestorEvent event) {}
        });

        settingsPanel.add(autoPrayerCheckbox);
        settingsPanel.add(Box.createVerticalStrut(5));
        settingsPanel.add(oneTickPrayerCheckbox);

        return settingsPanel;
    }

    @Provides
    OttoLiteConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(OttoLiteConfig.class);
    }

    @Provides
    OttoAgilityConfig provideAgilityConfig(ConfigManager configManager) {
        return configManager.getConfig(OttoAgilityConfig.class);
    }

    @Provides
    OttoScurriusConfig provideScurriusConfig(ConfigManager configManager) {
        return configManager.getConfig(OttoScurriusConfig.class);
    }

    @Provides
    OttoThievingConfig provideThievingConfig(ConfigManager configManager) {
        return configManager.getConfig(OttoThievingConfig.class);
    }

    @Provides
    OttoGauntletConfig provideGauntletConfig(ConfigManager configManager) {
        return configManager.getConfig(OttoGauntletConfig.class);
    }

    public OttoLitePanel getPanel() {
        return panel;
    }
}
