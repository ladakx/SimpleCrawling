package me.ladakx.simplecrawling.update;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.ladakx.simplecrawling.SimpleCrawling;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public final class UpdateChecker implements Listener {

    private static final String SPIGOT_API_URL = "https://api.spigotmc.org/legacy/update.php?resource=";
    private static final String RESOURCE_ID = "133784";
    private static final long CHECK_INTERVAL_TICKS = 72000L;
    private static final String DOWNLOAD_URL = "https://modrinth.com/plugin/simplecrawling";

    private final SimpleCrawling plugin;

    private TaskImplementation<Void> task;
    private volatile boolean updateAvailable;
    private volatile String latestVersion = "";

    public UpdateChecker(SimpleCrawling plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        stop();

        this.updateAvailable = false;
        this.latestVersion = "";

        if (!isEnabled()) {
            return;
        }

        checkNow();
        this.task = plugin.getSchedulerAdapter().runAsyncTimer(this::check, CHECK_INTERVAL_TICKS * 50L, CHECK_INTERVAL_TICKS * 50L, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void checkNow() {
        plugin.getSchedulerAdapter().runAsync(this::check);
    }

    public void notifySender(CommandSender sender) {
        if (!updateAvailable) {
            return;
        }

        sender.sendMessage(plugin.getMessage("update-checker.available")
                .replace("{current}", plugin.getDescription().getVersion())
                .replace("{latest}", latestVersion));
        sender.sendMessage(plugin.getMessage("update-checker.download-link")
                .replace("{url}", DOWNLOAD_URL));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!isEnabled() || !plugin.getConfig().getBoolean("update-checker.notify-on-join", true)) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("simplecrawling.update-notify")) {
            return;
        }

        notifySender(player);
    }

    private void check() {
        try {
            String remoteVersion = fetchLatestVersion();
            latestVersion = remoteVersion;
            updateAvailable = !normalizeVersion(plugin.getDescription().getVersion())
                    .equalsIgnoreCase(normalizeVersion(remoteVersion));

            if (updateAvailable && plugin.getConfig().getBoolean("update-checker.log-to-console", true)) {
                plugin.getLogger().info("A new update is available: " + remoteVersion + " (current: " + plugin.getDescription().getVersion() + ")");
            }
        } catch (IOException exception) {
            if (plugin.getConfig().getBoolean("update-checker.log-failures", false)) {
                plugin.getLogger().warning("Failed to check for updates: " + exception.getMessage());
            }
        }
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("update-checker.enabled", true);
    }

    private String fetchLatestVersion() throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(SPIGOT_API_URL + RESOURCE_ID).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line = reader.readLine();
            return line == null ? "" : line.trim();
        } finally {
            connection.disconnect();
        }
    }

    private String normalizeVersion(String version) {
        if (version == null) {
            return "";
        }

        int separatorIndex = version.indexOf('-');
        if (separatorIndex >= 0) {
            return version.substring(0, separatorIndex).trim();
        }

        return version.trim();
    }
}
