package com.allfire.qqassist.command;

import com.allfire.qqassist.QQAssistPlugin;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class QQAssistCommand implements CommandExecutor {

    private final QQAssistPlugin plugin;

    public QQAssistCommand(QQAssistPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("qqassist.admin")) {
            sendMessage(sender, this.plugin.getConfigManager().getMessage("no_permission"));
            return true;
        }

        if (args.length == 0) {
            List<String> infoMessages = this.plugin.getConfig().getStringList("messages.info_message");
            for (String message : infoMessages) {
                sendMessage(sender, message.replace("%alias%", label));
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender, label);
                break;
            case "info":
                handleInfo(sender, label);
                break;
            default:
                sendMessage(sender, this.plugin.getConfigManager().getMessage("unknown_command"));
                return true;
        }
        return true;
    }

    private void handleReload(CommandSender sender, String label) {
        long startTime = System.currentTimeMillis();

        this.plugin.getConfigManager().loadConfig();

        long timeTaken = System.currentTimeMillis() - startTime;

        String message = this.plugin.getConfigManager().getMessage("reload_success").replace("%time%", String.valueOf(timeTaken)).replace("%alias%", label);

        sendMessage(sender, message);

        if (this.plugin.getConfigManager().isDebug()) {
            this.plugin.getLogger().info("Конфигурация перезагружена за " + timeTaken + "ms");
        }
    }

    private void handleInfo(CommandSender sender, String label) {
        List<String> infoMessages = this.plugin.getConfig().getStringList("messages.info_message");
        for (String message : infoMessages) {
            sendMessage(sender, message.replace("%alias%", label));
        }
    }

    private void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        String prefix = this.plugin.getConfigManager().getPrefix();
        message = message.replace("%prefix%", prefix);

        TextComponent textComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        sender.sendMessage(textComponent);
    }
}