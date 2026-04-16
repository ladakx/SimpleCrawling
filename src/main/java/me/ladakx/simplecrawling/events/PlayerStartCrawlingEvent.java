package me.ladakx.simplecrawling.events;

import me.ladakx.simplecrawling.crawl.CrawlCause;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PlayerStartCrawlingEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final CrawlCause cause;
    private boolean cancelled;

    public PlayerStartCrawlingEvent(Player player, CrawlCause cause) {
        this.player = player;
        this.cause = cause;
    }

    public Player getPlayer() {
        return player;
    }

    public CrawlCause getCause() {
        return cause;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
