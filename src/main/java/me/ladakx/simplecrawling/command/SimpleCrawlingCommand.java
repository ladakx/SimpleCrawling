package me.ladakx.simplecrawling.command;

import me.ladakx.simplecrawling.SimpleCrawling;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SimpleCrawlingCommand implements CommandExecutor {

    private static final String USAGE_MESSAGE = "command.usage";

    private final SimpleCrawling plugin;

    public SimpleCrawlingCommand(SimpleCrawling plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        if (subCommand.equals("reload")) {
            return handleReload(sender);
        }

        if (subCommand.equals("on") || subCommand.equals("off") || subCommand.equals("toggle")) {
            return handlePlayerAction(sender, label, args, subCommand);
        }

        sendUsage(sender, label);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("simplecrawling.reload")) {
            sender.sendMessage(plugin.getMessage("command.no-permission"));
            return true;
        }

        plugin.reloadPluginConfig();
        sender.sendMessage(plugin.getMessage("command.reload-success"));
        return true;
    }

    private boolean handlePlayerAction(CommandSender sender, String label, String[] args, String subCommand) {
        if (!sender.hasPermission("simplecrawling.manage")) {
            sender.sendMessage(plugin.getMessage("command.no-permission"));
            return true;
        }

        if (args.length != 2) {
            sendUsage(sender, label);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessage("command.player-not-found").replace("{player}", args[1]));
            return true;
        }

        plugin.getSchedulerAdapter().runEntity(target, () -> {
            String messagePath = getActionMessagePath(target, subCommand);
            sendFeedback(sender, plugin.getMessage(messagePath).replace("{player}", target.getName()));
        });
        return true;
    }

    private String getActionMessagePath(Player target, String subCommand) {
        switch (subCommand) {
            case "on":
                return plugin.forceStartCrawling(target) ? "command.crawl-enabled" : "command.already-crawling";
            case "off":
                return plugin.forceStopCrawling(target) ? "command.crawl-disabled" : "command.not-crawling";
            default:
                return plugin.isCrawling(target)
                        ? (plugin.forceStopCrawling(target) ? "command.crawl-disabled" : "command.not-crawling")
                        : (plugin.forceStartCrawling(target) ? "command.crawl-enabled" : "command.already-crawling");
        }
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(plugin.getMessage(USAGE_MESSAGE).replace("{label}", label));
    }

    private void sendFeedback(CommandSender sender, String message) {
        if (sender instanceof Player) {
            plugin.getSchedulerAdapter().runEntity((Player) sender, () -> sender.sendMessage(message));
        } else {
            plugin.getSchedulerAdapter().runGlobal(() -> sender.sendMessage(message));
        }
    }
}
