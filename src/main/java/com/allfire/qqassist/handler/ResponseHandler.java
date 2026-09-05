package com.allfire.qqassist.handler;

import com.allfire.qqassist.QQAssistPlugin;
import com.allfire.qqassist.config.ConfigManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ResponseHandler {

    private final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();
    private final QQAssistPlugin plugin;
    private final Map<UUID, Long> playerLastQuestion = new HashMap<>();
    private final Random random = new Random();

    public ResponseHandler(QQAssistPlugin plugin) {
        this.plugin = plugin;
    }

    public void processMessage(Player player, String message, String chatId) {
        UUID playerId = player.getUniqueId();
        Long lastQuestion = this.playerLastQuestion.get(playerId);
        if (lastQuestion != null && System.currentTimeMillis() - lastQuestion.longValue() < 500L) {
            return;
        }
        this.playerLastQuestion.put(playerId, Long.valueOf(System.currentTimeMillis()));

        Map<String, ConfigManager.RuleConfig> rules = this.plugin.getConfigManager().getRules();

        for (Map.Entry<String, ConfigManager.RuleConfig> entry : rules.entrySet()) {
            ConfigManager.RuleConfig rule = entry.getValue();

            if (!rule.allowedChats.isEmpty() && !rule.allowedChats.contains(chatId)) {
                continue;
            }
            if (!rule.permission.isEmpty() && !player.hasPermission(rule.permission)) {
                continue;
            }
            if (isOnCooldown(playerId, rule.name, rule.cooldownTicks)) {
                continue;
            }

            if (matchesQuestion(message, rule)) {

                Map<Integer, String> argValues = new HashMap<>();
                if (rule.argsDef != null && !rule.argsDef.isEmpty()) {
                    String lowerMessage = message.toLowerCase();
                    for (int i = 0; i < rule.argsDef.size(); i++) {
                        ConfigManager.RuleConfig.ArgDef argDef = rule.argsDef.get(i);
                        String foundValue = argDef.defaultValue;
                        for (String val : argDef.values) {
                            if (lowerMessage.contains(val.toLowerCase())) {
                                foundValue = val;
                                break;
                            }
                        }
                        argValues.put(Integer.valueOf(i + 1), foundValue);
                    }
                    this.plugin.getExpansion().setArgValues(player.getUniqueId(), argValues);
                }

                if (rule.sessionEnabled && !rule.sessionSteps.isEmpty()) {
                    String target = this.plugin.getExpansion().getTargetPlayerName(player.getUniqueId());
                    if (target == null) target = player.getName();
                    this.plugin.getSessionManager().startSession(player, rule, chatId, target);
                    setCooldown(player.getUniqueId(), rule.name);

                    break;
                }
                if (rule.chance < 100 && this.random.nextInt(100) >= rule.chance) {
                    continue;
                }

                int delayTicks = Math.max(rule.delayTicks, 0);
                if (delayTicks > 0) {
                    Bukkit.getScheduler().runTaskLater((Plugin) this.plugin, () -> executeResponses(player, rule), delayTicks);
                } else {
                    executeResponses(player, rule);
                }

                setCooldown(playerId, rule.name);
                break;
            }
        }
    }

    private boolean matchesQuestion(String message, ConfigManager.RuleConfig rule) {
        String lowerMessage = message.toLowerCase().trim();
        for (String exact : rule.questionsExact) {
            if (lowerMessage.equals(exact.toLowerCase().trim())) return true;
        }
        for (String contains : rule.questionsContains) {
            if (lowerMessage.contains(contains.toLowerCase().trim())) return true;
        }
        for (Pattern pattern : rule.questionsRegex) {
            if (pattern.matcher(message).find()) return true;
        }
        return false;
    }

    private void executeResponses(Player player, ConfigManager.RuleConfig rule) {
        for (String answer : rule.answers) {
            if (answer == null || answer.isEmpty()) {
                continue;
            }
            executeAction(player, answer);
        }
        if (!rule.randomAnswers.isEmpty()) {
            String randomAnswer = rule.randomAnswers.get(this.random.nextInt(rule.randomAnswers.size()));
            executeAction(player, randomAnswer);
        }
    }

    public void executeAction(Player player, String action) {
        if (action == null || action.isEmpty()) {
            return;
        }
        try {
            action = action.replace("%prefix%", this.plugin.getConfigManager().getPrefix());

            if (action.startsWith("arg:")) {
                handleArgAction(player, action);
                return;
            }

            if (action.startsWith("delay:")) {
                handleDelayAction(player, action);
                return;
            }

            if (action.contains("check:[")) {
                handleCheckAction(player, action);
                return;
            }

            if (action.startsWith("sound!")) {
                handleSound(player, action.substring(6).trim());
            } else if (action.startsWith("gSound!")) {
                handleGlobalSound(player, action.substring(7).trim());
            } else if (action.startsWith("asConsole!")) {
                handleConsoleCommand(player, action.substring(10).trim());
            } else if (action.startsWith("asPlayer!")) {
                handlePlayerCommand(player, action.substring(9).trim());
            } else if (action.startsWith("gMessage!")) {
                handleGlobalMessage(player, action.substring(9).trim());
            } else if (action.startsWith("message!")) {
                handleMessage(player, action.substring(8).trim());
            } else if (action.startsWith("actionbar:")) {
                handleActionbarWithDuration(player, action);
            } else if (action.startsWith("actionbar!")) {
                handleActionbar(player, action.substring(10).trim(), 60);
            } else if (action.startsWith("title:")) {
                handleTitleWithTimings(player, action);
            } else if (action.startsWith("title!")) {
                handleTitle(player, action.substring(6).trim(), 20, 40, 20);
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning("Ошибка при выполнении действия: " + action);
        }
    }

    private void handleArgAction(Player player, String action) {
        String remaining = action;
        Map<Integer, String> argValues = this.plugin.getExpansion().getArgValues(player.getUniqueId());

        while (remaining.startsWith("arg:")) {
            int bracketStart = remaining.indexOf("[");
            int bracketEnd = remaining.indexOf("]!");
            if (bracketStart == -1 || bracketEnd == -1) {
                return;
            }
            try {
                String numStr = remaining.substring(4, bracketStart).trim();
                if (numStr.endsWith(":")) {
                    numStr = numStr.substring(0, numStr.length() - 1);
                }
                int argNum = Integer.parseInt(numStr);
                String valuesStr = remaining.substring(bracketStart + 1, bracketEnd);

                String actualValue = argValues.getOrDefault(Integer.valueOf(argNum), "");
                if (!checkArgMatch(player, valuesStr, actualValue)) {
                    return;
                }
                remaining = remaining.substring(bracketEnd + 2).trim();
            } catch (NumberFormatException e) {
                return;
            }
        }

        if (!remaining.isEmpty()) {
            executeAction(player, remaining);
        }
    }

    private void handleDelayAction(Player player, String action) {
        try {
            String[] parts = action.substring(6).split("!", 2);
            String delayStr = parts[0].trim();
            int delayTicks = Integer.parseInt(delayStr);

            if (parts.length > 1) {
                String delayedAction = parts[1].trim();
                Bukkit.getScheduler().runTaskLater((Plugin) this.plugin, () -> executeAction(player, delayedAction), delayTicks);
            }
        } catch (NumberFormatException e) {
            this.plugin.getLogger().warning("Ошибка парсинга задержки в действии: " + action);
        }
    }

    private void handleCheckAction(Player player, String action) {
        int bracketStart = action.indexOf("check:[") + 7;
        int bracketEnd = action.indexOf("]!", bracketStart);
        if (bracketEnd == -1) {
            this.plugin.getLogger().warning("Неверный формат check действия: " + action);
            return;
        }
        String condition = action.substring(bracketStart, bracketEnd);
        String restAction = action.substring(bracketEnd + 2).trim();

        if (evaluateCondition(player, condition)) {
            executeAction(player, restAction);
        }
    }

    private void handleActionbarWithDuration(Player player, String action) {
        try {
            String[] parts = action.substring(10).split("!", 2);
            String durationStr = parts[0].trim();
            int duration = Integer.parseInt(durationStr);

            if (parts.length > 1) {
                handleActionbar(player, parts[1].trim(), duration);
            }
        } catch (NumberFormatException e) {
            this.plugin.getLogger().warning("Ошибка парсинга длительности actionbar: " + action);
        }
    }

    private void handleTitleWithTimings(Player player, String action) {
        try {
            String[] parts = action.substring(6).split("!", 2);
            if (parts.length > 1) {
                String[] times = parts[0].split(":");
                if (times.length < 3) {
                    this.plugin.getLogger().warning("Неверный формат title: требуется 3 параметра времени (fadeIn:stay:fadeOut)");
                    return;
                }
                int fadeIn = Integer.parseInt(times[0].trim());
                int stay = Integer.parseInt(times[1].trim());
                int fadeOut = Integer.parseInt(times[2].trim());
                handleTitle(player, parts[1].trim(), fadeIn, stay, fadeOut);
            }
        } catch (NumberFormatException e) {
            this.plugin.getLogger().warning("Ошибка парсинга параметров title: " + action);
        }
    }

    private boolean checkArgMatch(Player player, String expected, String actual) {
        if (expected.equals("*")) return true;
        if (expected.startsWith("regex:")) {
            try {
                return actual.matches(expected.substring(6));
            } catch (Exception e) {
                this.plugin.getLogger().warning("Ошибка в regex: " + expected);
                return false;
            }
        }
        if (expected.startsWith("papi:")) {
            String papiValue = PlaceholderAPI.setPlaceholders(player, expected.substring(5));
            return actual.equalsIgnoreCase(papiValue);
        }
        if (expected.startsWith("check:")) {
            return evaluateCondition(player, expected.substring(6));
        }
        if (expected.startsWith("contains:")) {
            return actual.toLowerCase().contains(expected.substring(9).toLowerCase());
        }
        if (expected.contains("||")) {
            for (String val : expected.split("\\|\\|")) {
                if (val.trim().equalsIgnoreCase(actual)) return true;
            }
            return false;
        }
        return expected.equalsIgnoreCase(actual);
    }

    private Component parseMessage(String message) {
        message = message.replaceAll("\\{#([A-Fa-f0-9]{6})>\\}(.*?)\\{#([A-Fa-f0-9]{6})<\\}", "<gradient:#$1:#$3>$2</gradient>");

        if (message.contains("<gradient:") || message.contains("<#") || message.contains("<color:")) {
            try {
                return MiniMessage.miniMessage().deserialize(message);
            } catch (Exception exception) {
            }
        }
        return (Component) LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    private String formatForCommand(String text, Player player) {
        text = LegacyComponentSerializer.legacyAmpersand().serialize(parseMessage(text));
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    private Component formatForDisplay(String text, Player player) {
        text = PlaceholderAPI.setPlaceholders(player, text);
        return parseMessage(text);
    }

    private void handleSound(Player player, String soundParams) {
        try {
            String[] parts = soundParams.split(" ");
            if (parts.length < 3) {
                this.plugin.getLogger().warning("Неверный формат звука: требуется НАЗВАНИЕ ГРОМКОСТЬ ТОН");
                return;
            }
            Sound sound = Sound.valueOf(parts[0]);
            float volume = Float.parseFloat(parts[1]);
            float pitch = Float.parseFloat(parts[2]);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            this.plugin.getLogger().warning("Неизвестный звук: " + soundParams);
        } catch (Exception e) {
            this.plugin.getLogger().warning("Ошибка воспроизведения звука: " + soundParams);
        }
    }

    private void handleGlobalSound(Player player, String soundParams) {
        try {
            String[] parts = soundParams.split(" ");
            if (parts.length < 3) {
                this.plugin.getLogger().warning("Неверный формат глобального звука: требуется НАЗВАНИЕ ГРОМКОСТЬ ТОН");
                return;
            }
            Sound sound = Sound.valueOf(parts[0]);
            float volume = Float.parseFloat(parts[1]);
            float pitch = Float.parseFloat(parts[2]);
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.playSound(onlinePlayer.getLocation(), sound, volume, pitch);
            }
        } catch (IllegalArgumentException e) {
            this.plugin.getLogger().warning("Неизвестный звук: " + soundParams);
        } catch (Exception e) {
            this.plugin.getLogger().warning("Ошибка воспроизведения глобального звука: " + soundParams);
        }
    }

    private void handleConsoleCommand(Player player, String command) {
        command = formatForCommand(command, player);
        String finalCommand = command;
        Bukkit.getScheduler().runTask((Plugin) this.plugin, () -> Bukkit.dispatchCommand((CommandSender) Bukkit.getConsoleSender(), finalCommand));
    }

    private void handlePlayerCommand(Player player, String command) {
        command = formatForCommand(command, player);
        String finalCommand = command;
        Bukkit.getScheduler().runTask((Plugin) this.plugin, () -> player.performCommand(finalCommand));
    }

    private void handleGlobalMessage(Player player, String message) {
        Bukkit.broadcast(formatForDisplay(message, player));
    }

    private void handleMessage(Player player, String message) {
        player.sendMessage(formatForDisplay(message, player));
    }

    private void handleActionbar(Player player, String message, int duration) {
        Component component = formatForDisplay(message, player);
        player.sendActionBar(component);
        int refreshTicks = this.plugin.getConfig().getInt("settings.actionbar-refresh-ticks", 20);
        if (duration > refreshTicks && refreshTicks > 0) {
            int repeats = duration / refreshTicks;
            for (int i = 1; i <= repeats; i++) {
                Bukkit.getScheduler().runTaskLater((Plugin) this.plugin, () -> player.sendActionBar(component), i * refreshTicks);
            }
        }
    }

    private void handleTitle(Player player, String titleText, int fadeIn, int stay, int fadeOut) {
        String text = PlaceholderAPI.setPlaceholders(player, titleText);

        TextComponent textComponent = Component.empty();
        Component title;
        Component subtitle;
        if (text.contains("\\n")) {
            String[] parts = text.split("\\\\n", 2);
            title = parseMessage(parts[0]);
            subtitle = parseMessage(parts[1]);
        } else {
            title = parseMessage(text);
            subtitle = textComponent;
        }
        Title.Times times = Title.Times.times(
                Ticks.duration(fadeIn),
                Ticks.duration(stay),
                Ticks.duration(fadeOut));

        player.showTitle(Title.title(title, subtitle, times));
    }

    private boolean isOnCooldown(UUID playerId, String ruleName, int cooldownTicks) {
        Map<String, Long> cooldowns = this.playerCooldowns.get(playerId);
        if (cooldowns == null) return false;
        Long cooldownUntil = cooldowns.get(ruleName);
        if (cooldownUntil == null) return false;
        return (System.currentTimeMillis() < cooldownUntil.longValue());
    }

    private void setCooldown(UUID playerId, String ruleName) {
        ((Map<String, Long>) this.playerCooldowns
                .computeIfAbsent(playerId, k -> new HashMap<>()))
                .put(ruleName, Long.valueOf(System.currentTimeMillis() + 1000L));
    }

    private boolean evaluateCondition(Player player, String condition) {
        condition = PlaceholderAPI.setPlaceholders(player, condition);
        String[] ops = { ">=", "<=", "!=", "!<-", "!|-", "!-|", "<-", "|-", "-|", ">", "<", "=" };
        for (String op : ops) {
            int idx = condition.indexOf(op);
            if (idx != -1) {
                String left = condition.substring(0, idx).trim();
                String right = condition.substring(idx + op.length()).trim();
                try {
                    double leftNum = Double.parseDouble(left);
                    double rightNum = Double.parseDouble(right);
                    if (op.equals(">=")) return leftNum >= rightNum;
                    if (op.equals(">")) return leftNum > rightNum;
                    if (op.equals("<=")) return leftNum <= rightNum;
                    if (op.equals("<")) return leftNum < rightNum;
                    if (op.equals("=")) return left.equals(right);
                    if (op.equals("!=")) return !left.equals(right);
                    return false;
                } catch (NumberFormatException e) {
                    if (op.equals("=")) return left.equalsIgnoreCase(right);
                    if (op.equals("!=")) return !left.equalsIgnoreCase(right);
                    if (op.equals("<-")) return left.toLowerCase().contains(right.toLowerCase());
                    if (op.equals("!<-")) return !left.toLowerCase().contains(right.toLowerCase());
                    if (op.equals("|-")) return left.toLowerCase().startsWith(right.toLowerCase());
                    if (op.equals("!|-")) return !left.toLowerCase().startsWith(right.toLowerCase());
                    if (op.equals("-|")) return left.toLowerCase().endsWith(right.toLowerCase());
                    if (op.equals("!-|")) return !left.toLowerCase().endsWith(right.toLowerCase());
                    return false;
                }
            }
        }

        return false;
    }
}