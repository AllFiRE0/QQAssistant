package com.allfire.qqassist.chat;

import com.allfire.qqassist.QQAssistPlugin;
import com.allfire.qqassist.config.ConfigManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SessionManager {

    private final Map<UUID, SessionData> sessions = new ConcurrentHashMap<>();
    private final QQAssistPlugin plugin;
    private final Map<UUID, Integer> scheduledTasks = new ConcurrentHashMap<>();

    public SessionManager(QQAssistPlugin plugin) {
        this.plugin = plugin;
    }

    public void startSession(Player player, ConfigManager.RuleConfig rule, String chatId, String target) {
        SessionData old = this.sessions.remove(player.getUniqueId());
        if (old != null) {
            cancelReminder(player.getUniqueId());
        }
        SessionData session = new SessionData(rule, chatId, target);
        this.sessions.put(player.getUniqueId(), session);
        askStep(player, session);
    }

    public boolean handleSessionMessage(Player player, String message) {
        UUID playerId = player.getUniqueId();
        SessionData session = getSession(playerId);
        if (session == null) return false;

        String msg = message.trim();

        for (String trigger : session.rule.sessionCancelTriggers) {
            if (msg.equalsIgnoreCase(trigger)) {
                cancelSession(player);
                return true;
            }
        }

        for (String trigger : session.rule.sessionSkipTriggers) {
            if (msg.equalsIgnoreCase(trigger)) {
                ConfigManager.RuleConfig.SessionStep sessionStep = session.rule.sessionSteps.get(session.currentStep);
                if (sessionStep.defaultValue != null) {
                    session.args.put(sessionStep.id, sessionStep.defaultValue);
                    session.currentStep++;
                    if (session.currentStep < session.rule.sessionSteps.size()) {
                        askStep(player, session);
                    } else {
                        finishSession(player, session);
                    }
                }
                return true;
            }
        }

        String waitTime = parseWaitTime(msg);
        if (waitTime != null) {
            session.idleTimeout = parseWaitTimeMillis(msg);
            session.idleUntil = System.currentTimeMillis() + session.idleTimeout;
            this.plugin.getResponseHandler().executeAction(player, session.rule.sessionIdleMessage
                    .replace("%session_idle_left%", waitTime));
            scheduleReminder(player, session);
            return true;
        }

        String mentioned = this.plugin.getExpansion().getTargetPlayerName(playerId);
        if (mentioned != null && !mentioned.equals(session.target)) {
            session.target = mentioned;
            this.plugin.getResponseHandler().executeAction(player, "message! &#EA7AFF▏ &rЦель изменена на &e" + mentioned + "&r. Повторяю вопрос:");

            askStep(player, session);
            return true;
        }

        ConfigManager.RuleConfig.SessionStep step = session.rule.sessionSteps.get(session.currentStep);
        if (msg.matches(step.validate)) {
            session.args.put(step.id, msg);
            session.currentStep++;
            cancelReminder(playerId);
            if (session.currentStep < session.rule.sessionSteps.size()) {
                askStep(player, session);
            } else {
                finishSession(player, session);
            }
        } else if (step.errorMessage != null) {
            this.plugin.getResponseHandler().executeAction(player, step.errorMessage);
        }
        return true;
    }

    private void askStep(Player player, SessionData session) {
        cancelReminder(player.getUniqueId());
        ConfigManager.RuleConfig.SessionStep step = session.rule.sessionSteps.get(session.currentStep);

        String prompt = step.prompt.replace("%session_target%", session.target).replace("%session_current_step%", String.valueOf(session.currentStep + 1));
        this.plugin.getResponseHandler().executeAction(player, prompt);
        scheduleIdleTimeout(player, session);
    }

    private void scheduleIdleTimeout(Player player, SessionData session) {
        UUID playerId = player.getUniqueId();
        cancelReminder(playerId);

        if (session.rule.sessionIdleTimeout > 0) {
            session.idleUntil = System.currentTimeMillis() + session.rule.sessionIdleTimeout * 1000L;

            int taskId = Bukkit.getScheduler().runTaskLater((Plugin) this.plugin, () -> {
                ConfigManager.RuleConfig.SessionStep step = session.rule.sessionSteps.get(session.currentStep);
                this.plugin.getResponseHandler().executeAction(player, step.prompt.replace("%session_target%", session.target).replace("%session_current_step%", String.valueOf(session.currentStep + 1)));
            }, session.rule.sessionIdleTimeout * 20L).getTaskId();

            this.scheduledTasks.put(playerId, Integer.valueOf(taskId));
        }
    }

    private void scheduleReminder(Player player, SessionData session) {
        UUID playerId = player.getUniqueId();
        cancelReminder(playerId);

        if (session.idleTimeout > 0L) {

            int taskId = Bukkit.getScheduler().runTaskLater((Plugin) this.plugin, () -> {
                ConfigManager.RuleConfig.SessionStep step = session.rule.sessionSteps.get(session.currentStep);
                this.plugin.getResponseHandler().executeAction(player, step.prompt.replace("%session_target%", session.target).replace("%session_current_step%", String.valueOf(session.currentStep + 1)));
            }, session.idleTimeout / 50L).getTaskId();

            this.scheduledTasks.put(playerId, Integer.valueOf(taskId));
        }
    }

    private void cancelReminder(UUID playerId) {
        Integer taskId = this.scheduledTasks.remove(playerId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId.intValue());
        }
    }

    private void finishSession(Player player, SessionData session) {
        cancelReminder(player.getUniqueId());

        this.plugin.getExpansion().setArgs(player.getUniqueId(), session.args);
        this.plugin.getExpansion().setTargetPlayer(player.getUniqueId(), session.target);

        for (String answer : session.rule.answers) {
            this.plugin.getResponseHandler().executeAction(player, answer);
        }
        if (!session.rule.randomAnswers.isEmpty()) {
            String randomAnswer = session.rule.randomAnswers.get(new Random().nextInt(session.rule.randomAnswers.size()));
            this.plugin.getResponseHandler().executeAction(player, randomAnswer);
        }

        session.finished = true;
        int argsTimeout = (session.rule.sessionArgsTimeout > 0) ? session.rule.sessionArgsTimeout : 1200;
        session.argsKeepUntil = System.currentTimeMillis() + argsTimeout * 50L;
        Bukkit.getScheduler().runTaskLater((Plugin) this.plugin, () -> this.sessions.remove(player.getUniqueId()), argsTimeout);
    }

    public void cancelSession(Player player) {
        UUID playerId = player.getUniqueId();
        SessionData session = this.sessions.remove(playerId);
        cancelReminder(playerId);
        if (session != null && session.rule.sessionCancelMessage != null) {
            this.plugin.getResponseHandler().executeAction(player, session.rule.sessionCancelMessage);
        }
    }

    public boolean isInSession(UUID playerId) {
        return getSession(playerId) != null && !getSession(playerId).finished;
    }

    public SessionData getSession(UUID playerId) {
        SessionData session = this.sessions.get(playerId);
        if (session == null) return null;
        if (session.finished && System.currentTimeMillis() > session.argsKeepUntil) {
            this.sessions.remove(playerId);
            return null;
        }
        return session;
    }

    public void clearAll() {
        this.scheduledTasks.values().forEach(Bukkit.getScheduler()::cancelTask);
        this.scheduledTasks.clear();
        this.sessions.clear();
    }

    private String parseWaitTime(String message) {
        String lower = message.toLowerCase();
        if (lower.matches(".*подожди.*\\d+.*(минут|мин|секунд|сек|час|часов).*") || lower
                .matches(".*wait.*\\d+.*(min|sec|hour|minute|second).*")) {
            Pattern p = Pattern.compile("(\\d+)\\s*(минут|мин|секунд|сек|час|часов|min|sec|hour|minute|second)");
            Matcher m = p.matcher(lower);
            if (m.find()) {
                return m.group(1) + " " + m.group(1);
            }
        }
        return null;
    }

    private long parseWaitTimeMillis(String message) {
        String lower = message.toLowerCase();
        Pattern p = Pattern.compile("(\\d+)\\s*(минут|мин|секунд|сек|час|часов|min|sec|hour|minute|second)");
        Matcher m = p.matcher(lower);
        if (m.find()) {
            int num = Integer.parseInt(m.group(1));
            String unit = m.group(2);
            if (unit.matches("секунд|сек|sec|second")) return num * 1000L;
            if (unit.matches("минут|мин|min|minute")) return num * 60000L;
            if (unit.matches("час|часов|hour")) return num * 3600000L;
        }
        return 30000L;
    }

    public static class SessionData {
        public ConfigManager.RuleConfig rule;
        public String chatId;
        public String listenChat;
        public String target;
        public int currentStep = 0;
        public Map<String, String> args = new HashMap<>();
        public long idleTimeout = 0L;
        public long idleUntil = 0L;
        public boolean finished = false;
        public long argsKeepUntil = 0L;

        public SessionData(ConfigManager.RuleConfig rule, String chatId, String target) {
            this.rule = rule;
            this.chatId = chatId;
            this.listenChat = (rule.sessionListenChat != null) ? rule.sessionListenChat : "same";
            this.target = target;
        }
    }
}