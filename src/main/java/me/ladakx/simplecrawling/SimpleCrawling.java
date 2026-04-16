package me.ladakx.simplecrawling;

import me.ladakx.simplecrawling.bstats.Metrics;
import me.ladakx.simplecrawling.command.CrawlCommand;
import me.ladakx.simplecrawling.command.SimpleCrawlingCommand;
import me.ladakx.simplecrawling.crawl.CrawlCause;
import me.ladakx.simplecrawling.crawl.CrawlManager;
import me.ladakx.simplecrawling.crawl.CrawlingListener;
import me.ladakx.simplecrawling.update.UpdateChecker;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class SimpleCrawling extends JavaPlugin {

    private SchedulerAdapter schedulerAdapter;
    private CrawlManager crawlManager;
    private UpdateChecker updateChecker;
    private double hitboxOffset;
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveMessagesConfig();
        reloadPluginConfig();

        this.schedulerAdapter = new SchedulerAdapter(this);
        new Metrics(this, 30436);

        this.crawlManager = new CrawlManager(this);
        this.updateChecker = new UpdateChecker(this);

        getServer().getPluginManager().registerEvents(new CrawlingListener(crawlManager), this);
        getServer().getPluginManager().registerEvents(updateChecker, this);

        getCommand("simplecrawling").setExecutor(new SimpleCrawlingCommand(this));
        getCommand("crawl").setExecutor(new CrawlCommand(this));

        updateChecker.reload();
    }

    @Override
    public void onDisable() {
        if (crawlManager != null) {
            crawlManager.stop();
        }
        if (updateChecker != null) {
            updateChecker.stop();
        }
        if (schedulerAdapter != null) {
            schedulerAdapter.cancelTasks();
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        reloadMessagesConfig();
        this.hitboxOffset = getConfig().getDouble("hitbox-offset", 0.5D) + 0.5D;
        if (updateChecker != null) {
            updateChecker.reload();
        }
    }

    public double getHitboxOffset() {
        return hitboxOffset;
    }

    public boolean forceStartCrawling(org.bukkit.entity.Player player) {
        return crawlManager.startCrawling(player, CrawlCause.API);
    }

    public boolean startCrawling(org.bukkit.entity.Player player) {
        return crawlManager.startCrawling(player);
    }

    public boolean forceStopCrawling(org.bukkit.entity.Player player) {
        return crawlManager.forceStopCrawling(player, CrawlCause.API);
    }

    public boolean stopCrawling(org.bukkit.entity.Player player) {
        return crawlManager.stopCrawling(player);
    }

    public boolean isCrawling(org.bukkit.entity.Player player) {
        return crawlManager.isCrawling(player);
    }

    public String getMessage(String path) {
        String message = messagesConfig.getString(path, path);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public SchedulerAdapter getSchedulerAdapter() {
        return schedulerAdapter;
    }

    private void saveMessagesConfig() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
    }

    private void reloadMessagesConfig() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
}
