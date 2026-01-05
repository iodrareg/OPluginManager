package io.ottoscripts;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class OttoLitePanel extends PluginPanel {
    private static final String MAIN_PANEL = "main";
    private static final String SETTINGS_PANEL = "settings";

    private final Map<String, ToggleButton> featureToggles = new LinkedHashMap<>();
    private final Map<String, JPanel> featureSettingsPanels = new LinkedHashMap<>();
    private final JPanel featuresPanel;
    private final JPanel cardContainer;
    private final CardLayout cardLayout;
    private final JPanel settingsContainer;
    private final JPanel settingsContent;
    private final JLabel settingsTitle;

    public OttoLitePanel() {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        cardContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // === Main Panel ===
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel headerLabel = new JLabel("OttoLite");
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(headerLabel);
        mainPanel.add(Box.createVerticalStrut(10));

        featuresPanel = new JPanel();
        featuresPanel.setLayout(new BoxLayout(featuresPanel, BoxLayout.Y_AXIS));
        featuresPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        featuresPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(featuresPanel);

        // === Settings Panel ===
        settingsContainer = new JPanel(new BorderLayout());
        settingsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Settings header with back button
        JPanel settingsHeader = new JPanel(new BorderLayout());
        settingsHeader.setBackground(ColorScheme.DARK_GRAY_COLOR);
        settingsHeader.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton backButton = createBackButton();
        backButton.addActionListener(e -> showMainPanel());

        settingsTitle = new JLabel("Settings");
        settingsTitle.setForeground(Color.WHITE);
        settingsTitle.setFont(settingsTitle.getFont().deriveFont(Font.BOLD, 14f));

        settingsHeader.add(backButton, BorderLayout.WEST);
        settingsHeader.add(settingsTitle, BorderLayout.CENTER);

        settingsContent = new JPanel();
        settingsContent.setLayout(new BoxLayout(settingsContent, BoxLayout.Y_AXIS));
        settingsContent.setBackground(ColorScheme.DARK_GRAY_COLOR);
        settingsContent.setBorder(new EmptyBorder(0, 10, 10, 10));

        settingsContainer.add(settingsHeader, BorderLayout.NORTH);
        settingsContainer.add(settingsContent, BorderLayout.CENTER);

        cardContainer.add(mainPanel, MAIN_PANEL);
        cardContainer.add(settingsContainer, SETTINGS_PANEL);

        add(cardContainer, BorderLayout.CENTER);
    }

    public void addCategoryHeader(String categoryName) {
        // Add some spacing before category (except for first one)
        if (featuresPanel.getComponentCount() > 0) {
            featuresPanel.add(Box.createVerticalStrut(12));
        }

        // Category label
        JLabel categoryLabel = new JLabel(categoryName);
        categoryLabel.setForeground(new Color(242, 80, 129)); // Pink accent color
        categoryLabel.setFont(categoryLabel.getFont().deriveFont(Font.BOLD, 14f));
        categoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        featuresPanel.add(categoryLabel);

        // Separator line
        JSeparator separator = new JSeparator();
        separator.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        featuresPanel.add(Box.createVerticalStrut(3));
        featuresPanel.add(separator);
        featuresPanel.add(Box.createVerticalStrut(6));

        featuresPanel.revalidate();
        featuresPanel.repaint();
    }

    public void addFeatureToggle(String id, String name, boolean enabled, Consumer<Boolean> onToggle) {
        addFeatureToggle(id, name, enabled, onToggle, null);
    }

    public void addFeatureToggle(String id, String name, boolean enabled, Consumer<Boolean> onToggle, JPanel settingsPanel) {
        JPanel rowPanel = new JPanel(new BorderLayout(5, 0));
        rowPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        rowPanel.setBorder(new EmptyBorder(6, 8, 6, 8));
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(nameLabel.getFont().deriveFont(14f));

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        controlsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Cogwheel button
        JLabel cogwheel = createCogwheelIcon();
        cogwheel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cogwheel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showSettingsPanel(id, name);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                cogwheel.setForeground(new Color(242, 80, 129));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cogwheel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            }
        });

        ToggleButton toggle = new ToggleButton(enabled);
        toggle.setOnToggle(onToggle);

        controlsPanel.add(cogwheel);
        controlsPanel.add(toggle);

        rowPanel.add(nameLabel, BorderLayout.WEST);
        rowPanel.add(controlsPanel, BorderLayout.EAST);

        featureToggles.put(id, toggle);

        if (settingsPanel != null) {
            featureSettingsPanels.put(id, settingsPanel);
        } else {
            // Default empty settings panel
            JPanel defaultSettings = new JPanel();
            defaultSettings.setLayout(new BoxLayout(defaultSettings, BoxLayout.Y_AXIS));
            defaultSettings.setBackground(ColorScheme.DARK_GRAY_COLOR);

            JLabel noSettings = new JLabel("No settings available");
            noSettings.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            noSettings.setAlignmentX(Component.LEFT_ALIGNMENT);
            defaultSettings.add(noSettings);

            featureSettingsPanels.put(id, defaultSettings);
        }

        featuresPanel.add(rowPanel);
        featuresPanel.add(Box.createVerticalStrut(3));
        featuresPanel.revalidate();
        featuresPanel.repaint();
    }

    public void setFeatureSettingsPanel(String id, JPanel settingsPanel) {
        featureSettingsPanels.put(id, settingsPanel);
    }

    private JLabel createCogwheelIcon() {
        JLabel label = new JLabel("\u2699"); // Unicode gear symbol
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setFont(label.getFont().deriveFont(14f));
        return label;
    }

    private JButton createBackButton() {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw background on hover
                if (getModel().isRollover()) {
                    g2d.setColor(ColorScheme.DARKER_GRAY_HOVER_COLOR);
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                }

                // Draw arrow
                g2d.setColor(getModel().isRollover() ? new Color(242, 80, 129) : ColorScheme.LIGHT_GRAY_COLOR);
                g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int centerY = getHeight() / 2;
                int arrowSize = 6;
                int tipX = (getWidth() / 2) - 3;

                // Arrow shape pointing left: <
                g2d.drawLine(tipX, centerY, tipX + arrowSize, centerY - arrowSize);
                g2d.drawLine(tipX, centerY, tipX + arrowSize, centerY + arrowSize);

                g2d.dispose();
            }
        };

        button.setPreferredSize(new Dimension(24, 24));
        button.setMaximumSize(new Dimension(24, 24));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        return button;
    }

    private void showMainPanel() {
        cardLayout.show(cardContainer, MAIN_PANEL);
    }

    private void showSettingsPanel(String id, String name) {
        settingsTitle.setText(name);
        settingsContent.removeAll();

        JPanel panel = featureSettingsPanels.get(id);
        if (panel != null) {
            settingsContent.add(panel);
        }

        settingsContent.revalidate();
        settingsContent.repaint();
        cardLayout.show(cardContainer, SETTINGS_PANEL);
    }

    public void setFeatureEnabled(String id, boolean enabled) {
        ToggleButton toggle = featureToggles.get(id);
        if (toggle != null) {
            toggle.setSelected(enabled);
        }
    }

    public boolean isFeatureEnabled(String id) {
        ToggleButton toggle = featureToggles.get(id);
        return toggle != null && toggle.isSelected();
    }

    /**
     * Compact toggle button styled like RuneLite's switches
     */
    private static class ToggleButton extends JPanel {
        private static final int WIDTH = 28;
        private static final int HEIGHT = 14;
        private static final Color ON_COLOR = new Color(242, 80, 129);
        private static final Color OFF_COLOR = new Color(60, 60, 60);
        private static final Color KNOB_COLOR = new Color(220, 220, 220);

        private boolean selected;
        private Consumer<Boolean> onToggle;

        public ToggleButton(boolean selected) {
            this.selected = selected;
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setMaximumSize(new Dimension(WIDTH, HEIGHT));
            setMinimumSize(new Dimension(WIDTH, HEIGHT));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    toggle();
                }
            });
        }

        public void setOnToggle(Consumer<Boolean> onToggle) {
            this.onToggle = onToggle;
        }

        public void toggle() {
            selected = !selected;
            repaint();
            if (onToggle != null) {
                onToggle.accept(selected);
            }
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw track
            g2d.setColor(selected ? ON_COLOR : OFF_COLOR);
            g2d.fillRoundRect(0, 0, WIDTH, HEIGHT, HEIGHT, HEIGHT);

            // Draw knob
            int knobSize = HEIGHT - 2;
            int knobX = selected ? WIDTH - knobSize - 1 : 1;
            g2d.setColor(KNOB_COLOR);
            g2d.fillOval(knobX, 1, knobSize, knobSize);

            g2d.dispose();
        }
    }
}
