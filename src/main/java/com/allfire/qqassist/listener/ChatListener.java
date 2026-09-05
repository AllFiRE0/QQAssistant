package com.allfire.qqassist.listener;

import com.allfire.qqassist.QQAssistPlugin;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import ru.brikster.chatty.api.event.ChattyMessageEvent;

public class ChatListener implements Listener {

    private final QQAssistPlugin plugin;

    public ChatListener(QQAssistPlugin plugin) {
        this.plugin = plugin;
        log("Chatty: " + (Bukkit.getPluginManager().isPluginEnabled("Chatty") ? "да" : "нет"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChattyMessage(ChattyMessageEvent event) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Chatty")) {
            return;
        }
        Player player = event.getSender();
        String message = event.getPlainMessage();
        String chatId = event.getChat().getId();
        log("[Chatty] chatId=" + chatId + " msg=" + message);

        Bukkit.getScheduler().runTask(this.plugin, () -> handleChat(player, message, chatId));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (Bukkit.getPluginManager().isPluginEnabled("Chatty")) {
            return;
        }
        Player player = event.getPlayer();
        String message = event.getMessage();
        log("[Async] msg=" + message);

        Bukkit.getScheduler().runTask(this.plugin, () -> handleChat(player, message, "default"));
    }

    private void handleChat(Player player, String message, String chatId) {
        this.plugin.getExpansion().setLastMessage(player.getUniqueId(), message);

        if (this.plugin.getSessionManager().isInSession(player.getUniqueId())) {
            boolean handled = this.plugin.getSessionManager().handleSessionMessage(player, message);
            if (handled) {
                return;
            }
        }

        String mentionedPlayer = findMentionedPlayer(message, player.getName());
        log("[Chatty] foundMention: " + mentionedPlayer);

        if (mentionedPlayer != null) {
            this.plugin.getExpansion().setTargetPlayer(player.getUniqueId(), mentionedPlayer);
        } else {
            this.plugin.getExpansion().clearTargetPlayer(player.getUniqueId());
        }

        this.plugin.getResponseHandler().processMessage(player, message, chatId);
    }

    private String findMentionedPlayer(String message, String senderName) {
        List<Pattern> patterns = this.plugin.getConfigManager().getMentionPatterns();
        log("[findMention] patterns count: " + patterns.size() + " msg: " + message);

        for (Pattern pattern : patterns) {
            log("[findMention] trying pattern: " + pattern.pattern());
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String name = matcher.group(1);
                log("[findMention] raw name: '" + name + "'");
                name = name.replaceAll("[?!.,:;)\\]]+$", "");
                log("[findMention] cleaned name: '" + name + "'");
                if (name.length() > 0 && !name.equalsIgnoreCase(senderName)) {

                    Player onlineTarget = Bukkit.getPlayer(name);
                    if (onlineTarget != null && onlineTarget.isOnline()) {
                        log("[findMention] target player: " + name + " (online)");
                        return name;
                    }

                    OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
                    if (offline != null && offline.hasPlayedBefore()) {
                        log("[findMention] target player: " + name + " (offline)");
                        return name;
                    }
                    log("[findMention] target player: null");
                }
            }
        }
        return null;
    }

    private void log(String message) {
        if (this.plugin.getConfig().getBoolean("settings.debug", false)) {
            this.plugin.getLogger().info(message);
        }
    }
}