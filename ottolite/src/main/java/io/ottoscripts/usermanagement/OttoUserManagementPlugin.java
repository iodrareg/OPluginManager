package io.ottoscripts.usermanagement;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@PluginDescriptor(
    name = "Otto User Management",
    description = "Manage login credentials for quick account switching",
    tags = {"user", "login", "account", "otto", "ottolite"},
    enabledByDefault = true
)
public class OttoUserManagementPlugin extends Plugin {
    private static final String CONFIG_GROUP = "ottousermanagement";
    private static final String CONFIG_KEY_USERS = "storedUsers";

    @Inject
    private Client client;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ConfigManager configManager;

    private OttoUserManagementPanel panel;
    private NavigationButton navButton;

    @Override
    protected void startUp() throws Exception {
        log.info("Otto User Management started");

        panel = new OttoUserManagementPanel(this);

        // Load saved users
        loadUsers();

        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon_user_management.png");

        navButton = NavigationButton.builder()
            .tooltip("Otto User Management")
            .icon(icon != null ? icon : new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB))
            .priority(5)
            .panel(panel)
            .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Otto User Management stopped");
        clientToolbar.removeNavigation(navButton);
        panel = null;
        navButton = null;
    }

    /**
     * Fill login credentials into the client login screen.
     */
    public void fillCredentials(String username, String password) {
        if (client.getGameState() != GameState.LOGIN_SCREEN) {
            log.warn("Not on login screen, cannot fill credentials");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            client.setUsername(username);
            client.setPassword(password);
            log.info("Filled credentials for user: {}", username);
        });
    }

    /**
     * Add a new user and save to config.
     */
    public void addUser(String username, String password) {
        List<StoredUser> users = panel.getUsers();
        users.add(new StoredUser(username, password));
        saveUsers(users);
        panel.refreshUserList();
        log.info("Added user: {}", username);
    }

    /**
     * Remove a user and save to config.
     */
    public void removeUser(StoredUser user) {
        List<StoredUser> users = panel.getUsers();
        users.remove(user);
        saveUsers(users);
        panel.refreshUserList();
        log.info("Removed user: {}", user.getUsername());
    }

    /**
     * Save users to config (encoded).
     */
    private void saveUsers(List<StoredUser> users) {
        StringBuilder sb = new StringBuilder();
        for (StoredUser user : users) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            // Encode username:password in base64
            String encoded = Base64.getEncoder().encodeToString(
                (user.getUsername() + ":" + user.getPassword()).getBytes()
            );
            sb.append(encoded);
        }
        configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY_USERS, sb.toString());
    }

    /**
     * Load users from config.
     */
    private void loadUsers() {
        String stored = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY_USERS);
        List<StoredUser> users = new ArrayList<>();

        if (stored != null && !stored.isEmpty()) {
            String[] encodedUsers = stored.split("\\|");
            for (String encoded : encodedUsers) {
                try {
                    String decoded = new String(Base64.getDecoder().decode(encoded));
                    String[] parts = decoded.split(":", 2);
                    if (parts.length == 2) {
                        users.add(new StoredUser(parts[0], parts[1]));
                    }
                } catch (Exception e) {
                    log.warn("Failed to decode stored user", e);
                }
            }
        }

        panel.setUsers(users);
    }

    @Provides
    OttoUserManagementConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(OttoUserManagementConfig.class);
    }
}
