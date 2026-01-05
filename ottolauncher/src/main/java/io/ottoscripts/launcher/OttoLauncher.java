package io.ottoscripts.launcher;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class OttoLauncher {

    private static final String HIJACK_JAR_URL = "https://github.com/Ethan-Vann/Installer/releases/download/1.0/RuneLiteHijack.jar";
    private static final String HIJACK_JAR_NAME = "OttoLauncher.jar";
    private static final String HIJACK_MAIN_CLASS = "ca.arnah.runelite.LauncherHijack";
    private static final String SIDELOADED_PLUGINS_DIR = "sideloaded-plugins";

    public static void main(String[] args) {
        OttoLauncherFrame.launch();
    }

    public static void patchRuneLite(Path runelitePath, String authKey) throws Exception {
        if (!Files.exists(runelitePath)) {
            throw new IOException("RuneLite path does not exist: " + runelitePath);
        }

        downloadHijackJar(runelitePath);
        patchConfig(runelitePath);
        createSideloadedPluginsDir(runelitePath);
        saveAuthKey(runelitePath, authKey);
    }

    public static void launchRuneLite(Path runelitePath) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder processBuilder;

        if (os.contains("mac")) {
            processBuilder = new ProcessBuilder("open", "-a", "RuneLite");
        } else if (os.contains("win")) {
            Path runeliteExe = runelitePath.resolve("RuneLite.exe");
            if (!Files.exists(runeliteExe)) {
                throw new IOException("RuneLite.exe not found at: " + runeliteExe);
            }
            processBuilder = new ProcessBuilder(runeliteExe.toString());
        } else {
            // Linux - try to find runelite launcher
            Path runeliteJar = runelitePath.resolve("RuneLite.jar");
            if (Files.exists(runeliteJar)) {
                processBuilder = new ProcessBuilder("java", "-jar", runeliteJar.toString());
            } else {
                // Try flatpak or system runelite
                processBuilder = new ProcessBuilder("runelite");
            }
        }

        processBuilder.directory(runelitePath.toFile());
        processBuilder.start();
    }

    private static void downloadHijackJar(Path runelitePath) throws Exception {
        Path targetPath = runelitePath.resolve(HIJACK_JAR_NAME);

        try (ReadableByteChannel channel = Channels.newChannel(URI.create(HIJACK_JAR_URL).toURL().openStream());
             FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
            fos.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
        }
    }

    private static void patchConfig(Path runelitePath) throws IOException {
        Path configPath = runelitePath.resolve("config.json");

        if (!Files.exists(configPath)) {
            throw new IOException("config.json not found at: " + configPath);
        }

        // Backup original config
        Path backupPath = runelitePath.resolve("config.json.backup");
        if (!Files.exists(backupPath)) {
            Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Read and modify config
        JSONObject config;
        try (InputStream is = Files.newInputStream(configPath)) {
            config = new JSONObject(new JSONTokener(is));
        }

        // Set the hijack main class
        config.put("mainClass", HIJACK_MAIN_CLASS);

        // Update classpath
        JSONArray classPath = new JSONArray();
        classPath.put(HIJACK_JAR_NAME);
        classPath.put("RuneLite.jar");
        config.put("classPath", classPath);

        // Write modified config
        try (FileWriter writer = new FileWriter(configPath.toFile())) {
            writer.write(config.toString(2));
        }
    }

    private static void createSideloadedPluginsDir(Path runelitePath) throws IOException {
        Path sideloadedPluginsPath = runelitePath.resolve(SIDELOADED_PLUGINS_DIR);
        if (!Files.exists(sideloadedPluginsPath)) {
            Files.createDirectories(sideloadedPluginsPath);
        }
    }

    private static void saveAuthKey(Path runelitePath, String authKey) throws IOException {
        Path authKeyPath = runelitePath.resolve(SIDELOADED_PLUGINS_DIR).resolve(".authkey");
        Files.writeString(authKeyPath, authKey);
    }

    public static void restoreConfig(Path runelitePath) throws IOException {
        Path configPath = runelitePath.resolve("config.json");
        Path backupPath = runelitePath.resolve("config.json.backup");

        if (!Files.exists(backupPath)) {
            throw new IOException("No backup found to restore from.");
        }

        Files.copy(backupPath, configPath, StandardCopyOption.REPLACE_EXISTING);
    }
}
