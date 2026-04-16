package me.ladakx.simplecrawling.crawl;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CrawlingListener implements Listener {

    private static final long DOUBLE_SHIFT_WINDOW_MILLIS = 500L;

    private final CrawlManager crawlManager;
    private final Map<UUID, Long> doubleShiftCooldown = new HashMap<>();

    public CrawlingListener(CrawlManager crawlManager) {
        this.crawlManager = crawlManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onToggleShift(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }

        Player player = event.getPlayer();
        if (crawlManager.isCrawling(player)) {
            crawlManager.stopCrawling(player, CrawlCause.DOUBLE_SHIFT);
            return;
        }

        if (player.isSwimming() || player.isFlying() || player.isGliding()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        if (isDoubleShift(playerId)) {
            crawlManager.startCrawling(player, CrawlCause.DOUBLE_SHIFT);
            doubleShiftCooldown.remove(playerId);
            return;
        }

        doubleShiftCooldown.put(playerId, System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSwimToggle(EntityToggleSwimEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (crawlManager.isCrawling(player) && !event.isSwimming()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        crawlManager.forceStopCrawling(event.getPlayer(), CrawlCause.QUIT);
        doubleShiftCooldown.remove(event.getPlayer().getUniqueId());
    }

    private boolean isDoubleShift(UUID playerId) {
        Long lastPressTime = doubleShiftCooldown.get(playerId);
        return lastPressTime != null && System.currentTimeMillis() - lastPressTime <= DOUBLE_SHIFT_WINDOW_MILLIS;
    }
}
