package me.ladakx.simplecrawling.crawl;

import me.ladakx.simplecrawling.events.PlayerStartCrawlingEvent;
import me.ladakx.simplecrawling.events.PlayerStopCrawlingEvent;
import me.ladakx.simplecrawling.SimpleCrawling;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CrawlManager {

    private final SimpleCrawling plugin;
    private final Map<UUID, CrawlSession> sessions = new ConcurrentHashMap<>();

    public CrawlManager(SimpleCrawling plugin) {
        this.plugin = plugin;
    }

    public void stop() {
        for (UUID uuid : sessions.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                forceStopCrawling(player, CrawlCause.PLUGIN_DISABLE);
                continue;
            }

            CrawlSession session = sessions.remove(uuid);
            if (session != null) {
                session.stop();
            }
        }
    }

    public boolean startCrawling(Player player) {
        return startCrawling(player, CrawlCause.API);
    }

    public boolean startCrawling(Player player, CrawlCause cause) {
        UUID playerId = player.getUniqueId();
        if (sessions.containsKey(playerId)) {
            return false;
        }

        PlayerStartCrawlingEvent event = new PlayerStartCrawlingEvent(player, cause);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        CrawlSession session = new CrawlSession(plugin, player);
        if (sessions.putIfAbsent(playerId, session) != null) {
            return false;
        }

        session.start();
        return true;
    }

    public boolean stopCrawling(Player player) {
        return stopCrawling(player, CrawlCause.API);
    }

    public boolean stopCrawling(Player player, CrawlCause cause) {
        return stopCrawling(player, cause, true);
    }

    public boolean forceStopCrawling(Player player, CrawlCause cause) {
        return stopCrawling(player, cause, false);
    }

    private boolean stopCrawling(Player player, CrawlCause cause, boolean callEvent) {
        CrawlSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return false;
        }

        if (callEvent) {
            PlayerStopCrawlingEvent event = new PlayerStopCrawlingEvent(player, cause);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                sessions.put(player.getUniqueId(), session);
                return false;
            }
        }

        session.stop();
        return true;
    }

    public boolean isCrawling(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }
}
