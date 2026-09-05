package com.allfire.qqassist.expansion;

import com.allfire.qqassist.QQAssistPlugin;
import com.allfire.qqassist.chat.SessionManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class QQAssistExpansion extends PlaceholderExpansion {

    private final Map<UUID, String> targetPlayers = new ConcurrentHashMap<>();
    private final QQAssistPlugin plugin;
    private final Map<UUID, Long> lastTargetTime = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, String>> argValuesMap = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, String>> playerArgs = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastMessages = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private long sessionTimeout;
    private String defaultTargetName;
    private String defaultParse;
    private String defaultData;

    public QQAssistExpansion(QQAssistPlugin plugin) {
        this.plugin = plugin;
        loadDefaults();
    }

    private void loadDefaults() {
        this.sessionTimeout = this.plugin.getConfig().getLong("settings.session-timeout", 60L) * 1000L;
        this.defaultTargetName = this.plugin.getConfig().getString("settings.placeholder-defaults.target-name", "кто-то");
        this.defaultParse = this.plugin.getConfig().getString("settings.placeholder-defaults.target-parse", "не нашлось");
        this.defaultData = this.plugin.getConfig().getString("settings.placeholder-defaults.target-data", "—");
    }

    public String getAuthor() {
        return "AllF1RE";
    }

    public String getIdentifier() {
        return "qqassist";
    }

    public String getVersion() {
        return "1.0.0";
    }

    public List<String> getPlaceholders() {
        List<String> placeholders = new ArrayList<>();
        placeholders.add("%qqassist_prefix%");
        placeholders.add("%qqassist_message%");
        placeholders.add("%qqassist_target%");
        placeholders.add("%qqassist_random_target%");
        placeholders.add("%qqassist_random_online_target%");
        placeholders.add("%qqassist_session_target%");
        placeholders.add("%qqassist_session_arg_<название>%");
        placeholders.add("%qqassist_session_idle_left%");
        placeholders.add("%qqassist_session_current_step%");
        placeholders.add("%qqassist_arg_<номер>%");
        placeholders.add("%qqassist_parse_{placeholder}%");
        placeholders.add("%qqassist_target_uuid%");
        placeholders.add("%qqassist_target_world%");
        placeholders.add("%qqassist_target_health%");
        placeholders.add("%qqassist_target_max_health%");
        placeholders.add("%qqassist_target_level%");
        placeholders.add("%qqassist_target_gamemode%");
        placeholders.add("%qqassist_target_food%");
        placeholders.add("%qqassist_target_xp%");
        return placeholders;
    }

    public void setTargetPlayer(UUID playerId, String targetName) {
        this.targetPlayers.put(playerId, targetName);
        this.lastTargetTime.put(playerId, Long.valueOf(System.currentTimeMillis()));
    }

    public void clearTargetPlayer(UUID playerId) {
        this.targetPlayers.remove(playerId);
        this.lastTargetTime.remove(playerId);
    }

    public void setArgValues(UUID playerId, Map<Integer, String> values) {
        this.argValuesMap.put(playerId, values);
    }

    public void setArgs(UUID playerId, Map<String, String> args) {
        if (args != null && !args.isEmpty()) {
            this.playerArgs.put(playerId, args);
        }
    }

    public void setLastMessage(UUID playerId, String message) {
        this.lastMessages.put(playerId, message);
    }

    public Map<Integer, String> getArgValues(UUID playerId) {
        return this.argValuesMap.getOrDefault(playerId, Collections.emptyMap());
    }

    public String getTargetPlayerName(UUID playerId) {
        Long lastTime = this.lastTargetTime.get(playerId);
        if (lastTime != null && System.currentTimeMillis() - lastTime.longValue() > this.sessionTimeout) {
            this.targetPlayers.remove(playerId);
            this.lastTargetTime.remove(playerId);
            return null;
        }
        return this.targetPlayers.get(playerId);
    }

    public Player getTargetPlayer(UUID playerId) {
        String name = getTargetPlayerName(playerId);
        return (name != null) ? Bukkit.getPlayer(name) : null;
    }

    public String onRequest(OfflinePlayer p, String params) {
        loadDefaults();

        if (params.equals("prefix")) {
            return this.plugin.getConfigManager().getPrefix();
        }

        if (params.equals("message")) {
            if (p == null) return "";
            return this.lastMessages.getOrDefault(p.getUniqueId(), "");
        }

        if (params.equals("random_target")) {
            OfflinePlayer[] allPlayers = Bukkit.getOfflinePlayers();
            List<String> validNames = new ArrayList<>();
            for (OfflinePlayer op : allPlayers) {
                if (op.hasPlayedBefore() && op.getName() != null) {
                    validNames.add(op.getName());
                }
            }
            if (validNames.isEmpty()) return this.defaultTargetName;
            return validNames.get(this.random.nextInt(validNames.size()));
        }

        if (params.equals("random_online_target")) {
            Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
            if (onlinePlayers.isEmpty()) return this.defaultTargetName;
            List<Player> list = new ArrayList<>(onlinePlayers);
            return list.get(this.random.nextInt(list.size())).getName();
        }

        if (params.startsWith("arg_")) {
            if (p == null) return "";
            try {
                int num = Integer.parseInt(params.substring(4));
                return getArgValues(p.getUniqueId()).getOrDefault(Integer.valueOf(num), "");
            } catch (NumberFormatException e) {
                return "";
            }
        }

        if (params.startsWith("session_")) {
            if (p == null) return "";
            UUID playerId = p.getUniqueId();
            SessionManager.SessionData session = this.plugin.getSessionManager().getSession(playerId);
            if (session == null) return "";

            if (params.equals("session_target")) return session.target;
            if (params.equals("session_current_step")) return String.valueOf(session.currentStep + 1);
            if (params.equals("session_idle_left")) {
                long left = session.idleUntil - System.currentTimeMillis();
                if (left <= 0L) return "0 секунд";
                if (left >= 3600000L) return "" + left / 3600000L + " часов";
                if (left >= 60000L) return "" + left / 60000L + " минут";
                return "" + left / 1000L + " секунд";
            }
            if (params.startsWith("session_arg_")) {
                return session.args.getOrDefault(params.substring(12), "");
            }
            return "";
        }

        if (p == null || !p.isOnline()) return "";
        Player viewer = p.getPlayer();
        if (viewer == null) return "";
        UUID viewerId = viewer.getUniqueId();
        String targetName = getTargetPlayerName(viewerId);

        if (params.equals("target")) {
            if (targetName != null && !targetName.isEmpty()) return targetName;
            return viewer.getName();
        }

        Player target = getTargetPlayer(viewerId);

        if (params.startsWith("parse_")) {
            String result;
            String placeholderName = params.substring(6);
            if (targetName == null || targetName.isEmpty()) {
                result = PlaceholderAPI.setPlaceholders(viewer, "%" + placeholderName + "%");
                if (result == null || result.isEmpty() || result.equals("%" + placeholderName + "%")) return this.defaultParse;
                return result;
            }

            if (target != null) {
                result = PlaceholderAPI.setPlaceholders(target, "%" + placeholderName + "%");
            } else {
                OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                if (offlineTarget == null || !offlineTarget.hasPlayedBefore()) return this.defaultParse;
                result = PlaceholderAPI.setPlaceholders(offlineTarget, "%" + placeholderName + "%");
            }
            if (result == null || result.isEmpty() || result.equals("%" + placeholderName + "%")) return this.defaultParse;
            return result;
        }

        if (params.startsWith("target_")) {
            if (target == null) return this.defaultData;
            String dataType = params.substring(7);
            switch (dataType.toLowerCase()) {
                case "uuid":
                    return target.getUniqueId().toString();
                case "world":
                    return target.getWorld().getName();
                case "health":
                    return String.format("%.1f", target.getHealth());
                case "max_health":
                    return String.format("%.1f", target.getMaxHealth());
                case "level":
                    return String.valueOf(target.getLevel());
                case "gamemode":
                    return target.getGameMode().name();
                case "food":
                    return String.valueOf(target.getFoodLevel());
                case "xp":
                    return String.valueOf(target.getTotalExperience());
            }
            return this.defaultData;
        }

        return null;
    }

    public boolean persist() {
        return true;
    }

    public boolean canRegister() {
        return true;
    }
}