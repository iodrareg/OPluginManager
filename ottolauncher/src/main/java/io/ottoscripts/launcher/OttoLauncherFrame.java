package io.ottoscripts.launcher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class OttoLauncherFrame extends JFrame {

    // RuneLite Color Scheme
    private static final Color DARK_GRAY_COLOR = new Color(0x252525);
    private static final Color DARKER_GRAY_COLOR = new Color(0x1b1b1b);
    private static final Color DARK_GRAY_HOVER_COLOR = new Color(0x444444);
    private static final Color MEDIUM_GRAY_COLOR = new Color(0x3a3a3a);
    private static final Color LIGHT_GRAY_COLOR = new Color(0xa5a5a5);
    private static final Color BRAND_ORANGE = new Color(0xdc8a3f);
    private static final Color BRAND_ORANGE_HOVER = new Color(0xe69a4f);
    private static final Color PROGRESS_ERROR_COLOR = new Color(0xcc0000);
    private static final Color PROGRESS_COMPLETE_COLOR = new Color(0x0dc10d);
    private static final Color TEXT_COLOR = new Color(0xffffff);
    private static final Color BORDER_COLOR = new Color(0x1b1b1b);

    private static Font RUNESCAPE_SMALL_FONT;
    private static Font RUNESCAPE_FONT;
    private static Font RUNESCAPE_BOLD_FONT;

    private static final Color TITLE_BAR_COLOR = new Color(0x242424);
    private static final Color CLOSE_HOVER_COLOR = new Color(0xe81123);

    private final JTextField runelitePathField;
    private final JTextField authKeyField;
    private final JButton patchButton;
    private final JButton runClientButton;
    private final JLabel statusLabel;

    // For window dragging
    private Point dragOffset;

    static {
        try {
            InputStream fontStream = OttoLauncherFrame.class.getResourceAsStream("/runescape.ttf");
            if (fontStream != null) {
                Font runescapeBase = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(runescapeBase);
                RUNESCAPE_SMALL_FONT = runescapeBase.deriveFont(Font.PLAIN, 14f);
                RUNESCAPE_FONT = runescapeBase.deriveFont(Font.PLAIN, 16f);
                RUNESCAPE_BOLD_FONT = runescapeBase.deriveFont(Font.BOLD, 16f);
                fontStream.close();
            } else {
                setFallbackFonts();
            }
        } catch (Exception e) {
            setFallbackFonts();
        }
    }

    private static void setFallbackFonts() {
        RUNESCAPE_SMALL_FONT = new Font("Dialog", Font.PLAIN, 12);
        RUNESCAPE_FONT = new Font("Dialog", Font.PLAIN, 14);
        RUNESCAPE_BOLD_FONT = new Font("Dialog", Font.BOLD, 14);
    }

    public OttoLauncherFrame() {
        setTitle("OttoLauncher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setUndecorated(true);
        setBackground(DARKER_GRAY_COLOR);

        // Root panel that holds everything
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(DARKER_GRAY_COLOR);
        rootPanel.setBorder(new LineBorder(MEDIUM_GRAY_COLOR, 1));

        // Custom title bar
        JPanel titleBar = createTitleBar();
        rootPanel.add(titleBar, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));
        mainPanel.setBackground(DARKER_GRAY_COLOR);

        // Title with RuneLite styling
        JLabel titleLabel = new JLabel("OttoLauncher");
        titleLabel.setFont(RUNESCAPE_BOLD_FONT.deriveFont(28f));
        titleLabel.setForeground(BRAND_ORANGE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(4));

        JLabel subtitleLabel = new JLabel("RuneLite Plugin Sideloader");
        subtitleLabel.setFont(RUNESCAPE_SMALL_FONT);
        subtitleLabel.setForeground(LIGHT_GRAY_COLOR);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createVerticalStrut(25));

        // Separator
        mainPanel.add(createSeparator());
        mainPanel.add(Box.createVerticalStrut(20));

        // RuneLite Path
        JPanel pathPanel = new JPanel(new BorderLayout(10, 0));
        pathPanel.setMaximumSize(new Dimension(480, 40));
        pathPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        pathPanel.setBackground(DARKER_GRAY_COLOR);

        JLabel pathLabel = createStyledLabel("RuneLite Path:");
        pathLabel.setPreferredSize(new Dimension(110, 30));
        pathPanel.add(pathLabel, BorderLayout.WEST);

        runelitePathField = createStyledTextField(getDefaultRuneLitePath());
        pathPanel.add(runelitePathField, BorderLayout.CENTER);

        JButton browseButton = createStyledButton("Browse", false);
        browseButton.setPreferredSize(new Dimension(80, 30));
        browseButton.addActionListener(this::onBrowse);
        pathPanel.add(browseButton, BorderLayout.EAST);

        mainPanel.add(pathPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // Authentication Key
        JPanel authPanel = new JPanel(new BorderLayout(10, 0));
        authPanel.setMaximumSize(new Dimension(480, 40));
        authPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        authPanel.setBackground(DARKER_GRAY_COLOR);

        JLabel authLabel = createStyledLabel("Auth Key:");
        authLabel.setPreferredSize(new Dimension(110, 30));
        authPanel.add(authLabel, BorderLayout.WEST);

        authKeyField = createStyledTextField("");
        authPanel.add(authKeyField, BorderLayout.CENTER);

        mainPanel.add(authPanel);
        mainPanel.add(Box.createVerticalStrut(25));

        // Separator
        mainPanel.add(createSeparator());
        mainPanel.add(Box.createVerticalStrut(20));

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.setBackground(DARKER_GRAY_COLOR);

        patchButton = createStyledButton("Patch RuneLite", true);
        patchButton.setPreferredSize(new Dimension(160, 38));
        patchButton.addActionListener(this::onPatchRuneLite);
        buttonPanel.add(patchButton);

        runClientButton = createStyledButton("Run Client", true);
        runClientButton.setPreferredSize(new Dimension(160, 38));
        runClientButton.addActionListener(this::onRunClient);
        buttonPanel.add(runClientButton);

        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Status Panel
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
        statusPanel.setBackground(DARK_GRAY_COLOR);
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(MEDIUM_GRAY_COLOR, 1),
                new EmptyBorder(8, 15, 8, 15)
        ));
        statusPanel.setMaximumSize(new Dimension(480, 35));
        statusPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(RUNESCAPE_SMALL_FONT);
        statusLabel.setForeground(LIGHT_GRAY_COLOR);
        statusPanel.add(statusLabel);

        mainPanel.add(statusPanel);

        // Add main panel to root
        rootPanel.add(mainPanel, BorderLayout.CENTER);

        add(rootPanel);
        pack();
        setLocationRelativeTo(null);

        updateRunClientButtonState();
    }

    private JPanel createTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(TITLE_BAR_COLOR);
        titleBar.setPreferredSize(new Dimension(0, 32));
        titleBar.setBorder(new EmptyBorder(0, 10, 0, 0));

        // Title label on the left
        JLabel titleLabel = new JLabel("OttoLauncher");
        titleLabel.setFont(RUNESCAPE_SMALL_FONT);
        titleLabel.setForeground(LIGHT_GRAY_COLOR);
        titleBar.add(titleLabel, BorderLayout.WEST);

        // Window controls on the right
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        controlsPanel.setBackground(TITLE_BAR_COLOR);

        JButton minimizeBtn = createWindowButton("\u2013", DARK_GRAY_HOVER_COLOR);
        minimizeBtn.addActionListener(e -> setState(Frame.ICONIFIED));

        JButton closeBtn = createWindowButton("\u2715", CLOSE_HOVER_COLOR);
        closeBtn.addActionListener(e -> System.exit(0));

        controlsPanel.add(minimizeBtn);
        controlsPanel.add(closeBtn);
        titleBar.add(controlsPanel, BorderLayout.EAST);

        // Window dragging
        titleBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }
        });

        titleBar.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point currentLocation = getLocation();
                setLocation(
                    currentLocation.x + e.getX() - dragOffset.x,
                    currentLocation.y + e.getY() - dragOffset.y
                );
            }
        });

        return titleBar;
    }

    private JButton createWindowButton(String symbol, Color hoverColor) {
        JButton button = new JButton(symbol);
        button.setFont(new Font("Dialog", Font.PLAIN, 13));
        button.setForeground(LIGHT_GRAY_COLOR);
        button.setBackground(TITLE_BAR_COLOR);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(46, 32));
        button.setUI(new BasicButtonUI());
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
                if (hoverColor.equals(CLOSE_HOVER_COLOR)) {
                    button.setForeground(TEXT_COLOR);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(TITLE_BAR_COLOR);
                button.setForeground(LIGHT_GRAY_COLOR);
            }
        });

        return button;
    }

    private JSeparator createSeparator() {
        JSeparator separator = new JSeparator();
        separator.setForeground(MEDIUM_GRAY_COLOR);
        separator.setBackground(DARKER_GRAY_COLOR);
        separator.setMaximumSize(new Dimension(480, 2));
        return separator;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(RUNESCAPE_FONT);
        label.setForeground(LIGHT_GRAY_COLOR);
        return label;
    }

    private JTextField createStyledTextField(String text) {
        JTextField field = new JTextField(text);
        field.setFont(RUNESCAPE_SMALL_FONT);
        field.setForeground(TEXT_COLOR);
        field.setBackground(DARK_GRAY_COLOR);
        field.setCaretColor(TEXT_COLOR);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(MEDIUM_GRAY_COLOR, 1),
                new EmptyBorder(5, 8, 5, 8)
        ));
        field.setPreferredSize(new Dimension(200, 30));

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(BRAND_ORANGE, 1),
                        new EmptyBorder(5, 8, 5, 8)
                ));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(MEDIUM_GRAY_COLOR, 1),
                        new EmptyBorder(5, 8, 5, 8)
                ));
            }
        });

        return field;
    }

    private JButton createStyledButton(String text, boolean isPrimary) {
        JButton button = new JButton(text);
        button.setFont(RUNESCAPE_FONT);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setUI(new BasicButtonUI());

        if (isPrimary) {
            button.setBackground(BRAND_ORANGE);
            button.setForeground(TEXT_COLOR);
            button.setBorder(new LineBorder(BRAND_ORANGE, 1));
        } else {
            button.setBackground(DARK_GRAY_COLOR);
            button.setForeground(LIGHT_GRAY_COLOR);
            button.setBorder(new LineBorder(MEDIUM_GRAY_COLOR, 1));
        }

        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            Color originalBg = button.getBackground();
            Color originalFg = button.getForeground();

            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    if (isPrimary) {
                        button.setBackground(BRAND_ORANGE_HOVER);
                        button.setBorder(new LineBorder(BRAND_ORANGE_HOVER, 1));
                    } else {
                        button.setBackground(DARK_GRAY_HOVER_COLOR);
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    if (isPrimary) {
                        button.setBackground(BRAND_ORANGE);
                        button.setBorder(new LineBorder(BRAND_ORANGE, 1));
                    } else {
                        button.setBackground(DARK_GRAY_COLOR);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (button.isEnabled()) {
                    if (isPrimary) {
                        button.setBackground(BRAND_ORANGE.darker());
                    } else {
                        button.setBackground(MEDIUM_GRAY_COLOR);
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (button.isEnabled()) {
                    if (isPrimary) {
                        button.setBackground(BRAND_ORANGE);
                    } else {
                        button.setBackground(DARK_GRAY_COLOR);
                    }
                }
            }
        });

        return button;
    }

    private String getDefaultRuneLitePath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            return "/Applications/RuneLite.app/Contents/Resources";
        } else if (os.contains("win")) {
            return System.getProperty("user.home") + "\\AppData\\Local\\RuneLite";
        } else {
            return System.getProperty("user.home") + "/.runelite";
        }
    }

    private void updateRunClientButtonState() {
        Path runelitePath = Path.of(runelitePathField.getText());
        Path sideloadedPluginsPath = runelitePath.resolve("sideloaded-plugins");
        boolean isPatched = Files.exists(sideloadedPluginsPath);
        runClientButton.setEnabled(isPatched);

        if (!isPatched) {
            runClientButton.setBackground(DARK_GRAY_COLOR);
            runClientButton.setForeground(LIGHT_GRAY_COLOR.darker());
            runClientButton.setBorder(new LineBorder(MEDIUM_GRAY_COLOR, 1));
        } else {
            runClientButton.setBackground(BRAND_ORANGE);
            runClientButton.setForeground(TEXT_COLOR);
            runClientButton.setBorder(new LineBorder(BRAND_ORANGE, 1));
        }

        if (isPatched) {
            statusLabel.setText("Status: RuneLite is patched and ready");
            statusLabel.setForeground(PROGRESS_COMPLETE_COLOR);
        } else {
            statusLabel.setText("Status: RuneLite needs to be patched");
            statusLabel.setForeground(LIGHT_GRAY_COLOR);
        }
    }

    private void onBrowse(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(new File(runelitePathField.getText()));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            runelitePathField.setText(chooser.getSelectedFile().getAbsolutePath());
            updateRunClientButtonState();
        }
    }

    private void onPatchRuneLite(ActionEvent e) {
        String authKey = authKeyField.getText().trim();
        if (authKey.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter your authentication key.",
                    "Auth Key Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path runelitePath = Path.of(runelitePathField.getText());
        if (!Files.exists(runelitePath)) {
            JOptionPane.showMessageDialog(this,
                    "RuneLite path does not exist: " + runelitePath,
                    "Invalid Path",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        patchButton.setEnabled(false);
        patchButton.setBackground(DARK_GRAY_COLOR);
        patchButton.setForeground(LIGHT_GRAY_COLOR.darker());
        statusLabel.setText("Status: Patching RuneLite...");
        statusLabel.setForeground(BRAND_ORANGE);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground() {
                try {
                    OttoLauncher.patchRuneLite(runelitePath, authKey);
                    return true;
                } catch (Exception ex) {
                    errorMessage = ex.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                patchButton.setEnabled(true);
                patchButton.setBackground(BRAND_ORANGE);
                patchButton.setForeground(TEXT_COLOR);
                try {
                    if (get()) {
                        updateRunClientButtonState();
                        JOptionPane.showMessageDialog(OttoLauncherFrame.this,
                                "RuneLite has been patched successfully!",
                                "Patch Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        statusLabel.setText("Status: Patch failed");
                        statusLabel.setForeground(PROGRESS_ERROR_COLOR);
                        JOptionPane.showMessageDialog(OttoLauncherFrame.this,
                                "Failed to patch RuneLite: " + errorMessage,
                                "Patch Failed",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Status: Patch failed");
                    statusLabel.setForeground(PROGRESS_ERROR_COLOR);
                }
            }
        };
        worker.execute();
    }

    private void onRunClient(ActionEvent e) {
        Path runelitePath = Path.of(runelitePathField.getText());

        runClientButton.setEnabled(false);
        runClientButton.setBackground(DARK_GRAY_COLOR);
        runClientButton.setForeground(LIGHT_GRAY_COLOR.darker());
        statusLabel.setText("Status: Launching RuneLite...");
        statusLabel.setForeground(BRAND_ORANGE);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground() {
                try {
                    OttoLauncher.launchRuneLite(runelitePath);
                    return true;
                } catch (Exception ex) {
                    errorMessage = ex.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                runClientButton.setEnabled(true);
                runClientButton.setBackground(BRAND_ORANGE);
                runClientButton.setForeground(TEXT_COLOR);
                try {
                    if (get()) {
                        statusLabel.setText("Status: RuneLite launched");
                        statusLabel.setForeground(PROGRESS_COMPLETE_COLOR);
                    } else {
                        statusLabel.setText("Status: Launch failed");
                        statusLabel.setForeground(PROGRESS_ERROR_COLOR);
                        JOptionPane.showMessageDialog(OttoLauncherFrame.this,
                                "Failed to launch RuneLite: " + errorMessage,
                                "Launch Failed",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Status: Launch failed");
                    statusLabel.setForeground(PROGRESS_ERROR_COLOR);
                }
            }
        };
        worker.execute();
    }

    public static void launch() {
        // Use system look and feel defaults but apply our custom styling
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            // Set global UI properties for RuneLite theme
            UIManager.put("OptionPane.background", DARKER_GRAY_COLOR);
            UIManager.put("Panel.background", DARKER_GRAY_COLOR);
            UIManager.put("OptionPane.messageForeground", TEXT_COLOR);
            UIManager.put("Button.background", DARK_GRAY_COLOR);
            UIManager.put("Button.foreground", LIGHT_GRAY_COLOR);
        } catch (Exception e) {
            System.err.println("Failed to set look and feel");
        }

        SwingUtilities.invokeLater(() -> {
            OttoLauncherFrame frame = new OttoLauncherFrame();
            frame.setVisible(true);
        });
    }
}
