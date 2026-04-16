package me.ladakx.simplecrawling.command;

import me.ladakx.simplecrawling.SimpleCrawling;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CrawlCommand implements CommandExecutor {

    private final SimpleCrawling plugin;

    public CrawlCommand(SimpleCrawling plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("command.players-only"));
            return true;
        }

        if (!sender.hasPermission("simplecrawling.crawl")) {
            sender.sendMessage(plugin.getMessage("command.no-permission"));
            return true;
        }

        if (args.length != 0) {
            sender.sendMessage(plugin.getMessage("command.crawl-usage").replace("{label}", label));
            return true;
        }

        Player player = (Player) sender;
        if (plugin.isCrawling(player)) {
            player.sendMessage(plugin.forceStopCrawling(player)
                    ? plugin.getMessage("command.self-disabled")
                    : plugin.getMessage("command.not-crawling").replace("{player}", player.getName()));
            return true;
        }

        player.sendMessage(plugin.forceStartCrawling(player)
                ? plugin.getMessage("command.self-enabled")
                : plugin.getMessage("command.already-crawling").replace("{player}", player.getName()));
        return true;
    }
}
