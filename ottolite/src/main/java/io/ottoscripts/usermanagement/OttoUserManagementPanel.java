package io.ottoscripts.usermanagement;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class OttoUserManagementPanel extends PluginPanel {
    private final OttoUserManagementPlugin plugin;
    private final List<StoredUser> users = new ArrayList<>();
    private final JPanel usersListPanel;

    public OttoUserManagementPanel(OttoUserManagementPlugin plugin) {
        this.plugin = plugin;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Main content panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header
        JLabel headerLabel = new JLabel("User Management");
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(headerLabel);
        mainPanel.add(Box.createVerticalStrut(10));

        // Add User button
        JButton addUserButton = new JButton("Add User");
        addUserButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        addUserButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        addUserButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        addUserButton.setForeground(Color.WHITE);
        addUserButton.setFocusPainted(false);
        addUserButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        addUserButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addUserButton.addActionListener(e -> showAddUserDialog());
        mainPanel.add(addUserButton);
        mainPanel.add(Box.createVerticalStrut(15));

        // Users list header
        JLabel usersLabel = new JLabel("Saved Users");
        usersLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        usersLabel.setFont(usersLabel.getFont().deriveFont(Font.BOLD, 14f));
        usersLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(usersLabel);
        mainPanel.add(Box.createVerticalStrut(5));

        // Users list panel
        usersListPanel = new JPanel();
        usersListPanel.setLayout(new BoxLayout(usersListPanel, BoxLayout.Y_AXIS));
        usersListPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        usersListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(usersListPanel);

        // Add scroll pane
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(scrollPane, BorderLayout.CENTER);
    }

    private void showAddUserDialog() {
        JPanel dialogPanel = new JPanel(new GridBagLayout());
        dialogPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username field
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setForeground(Color.WHITE);
        dialogPanel.add(usernameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField usernameField = new JTextField(20);
        dialogPanel.add(usernameField, gbc);

        // Password field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(Color.WHITE);
        dialogPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPasswordField passwordField = new JPasswordField(20);
        dialogPanel.add(passwordField, gbc);

        int result = JOptionPane.showConfirmDialog(
            this,
            dialogPanel,
            "Add User",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (!username.isEmpty() && !password.isEmpty()) {
                plugin.addUser(username, password);
            }
        }
    }

    public void refreshUserList() {
        usersListPanel.removeAll();

        if (users.isEmpty()) {
            JLabel noUsersLabel = new JLabel("No users saved");
            noUsersLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            noUsersLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            usersListPanel.add(noUsersLabel);
        } else {
            for (StoredUser user : users) {
                usersListPanel.add(createUserRow(user));
                usersListPanel.add(Box.createVerticalStrut(3));
            }
        }

        usersListPanel.revalidate();
        usersListPanel.repaint();
    }

    private JPanel createUserRow(StoredUser user) {
        JPanel rowPanel = new JPanel(new BorderLayout(5, 0));
        rowPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        rowPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Username label
        JLabel nameLabel = new JLabel(user.getUsername());
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(nameLabel.getFont().deriveFont(14f));

        // Delete button
        JLabel deleteButton = new JLabel("\u2715"); // X symbol
        deleteButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        deleteButton.setFont(deleteButton.getFont().deriveFont(14f));
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                e.consume(); // Prevent row click
                int confirm = JOptionPane.showConfirmDialog(
                    OttoUserManagementPanel.this,
                    "Remove user '" + user.getUsername() + "'?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    plugin.removeUser(user);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                deleteButton.setForeground(new Color(255, 80, 80));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                deleteButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            }
        });

        rowPanel.add(nameLabel, BorderLayout.WEST);
        rowPanel.add(deleteButton, BorderLayout.EAST);

        // Click on row to fill credentials
        rowPanel.addMouseListener(new MouseAdapter() {
            private Color originalBg = ColorScheme.DARKER_GRAY_COLOR;

            @Override
            public void mouseClicked(MouseEvent e) {
                plugin.fillCredentials(user.getUsername(), user.getPassword());
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                rowPanel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                rowPanel.setBackground(originalBg);
            }
        });

        return rowPanel;
    }

    public List<StoredUser> getUsers() {
        return users;
    }

    public void setUsers(List<StoredUser> users) {
        this.users.clear();
        this.users.addAll(users);
        refreshUserList();
    }
}
