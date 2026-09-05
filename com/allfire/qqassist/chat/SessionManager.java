/*     */ package com.allfire.qqassist.chat;
/*     */ import com.allfire.qqassist.QQAssistPlugin;
/*     */ import com.allfire.qqassist.config.ConfigManager;
/*     */ import java.util.Map;
/*     */ import java.util.Random;
/*     */ import java.util.UUID;
/*     */ import java.util.concurrent.ConcurrentHashMap;
/*     */ import java.util.regex.Matcher;
/*     */ import java.util.regex.Pattern;
/*     */ import org.bukkit.Bukkit;
/*     */ import org.bukkit.entity.Player;
/*     */ import org.bukkit.plugin.Plugin;
/*     */ 
/*     */ public class SessionManager {
/*  15 */   private final Map<UUID, SessionData> sessions = new ConcurrentHashMap<>(); private final QQAssistPlugin plugin;
/*  16 */   private final Map<UUID, Integer> scheduledTasks = new ConcurrentHashMap<>();
/*     */   
/*     */   public SessionManager(QQAssistPlugin plugin) {
/*  19 */     this.plugin = plugin;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public void startSession(Player player, ConfigManager.RuleConfig rule, String chatId, String target) {
/*  25 */     SessionData old = this.sessions.remove(player.getUniqueId());
/*  26 */     if (old != null) {
/*  27 */       cancelReminder(player.getUniqueId());
/*     */     }
/*  29 */     SessionData session = new SessionData(rule, chatId, target);
/*  30 */     this.sessions.put(player.getUniqueId(), session);
/*  31 */     askStep(player, session);
/*     */   }
/*     */ 
/*     */   
/*     */   public boolean handleSessionMessage(Player player, String message) {
/*  36 */     UUID playerId = player.getUniqueId();
/*  37 */     SessionData session = getSession(playerId);
/*  38 */     if (session == null) return false;
/*     */     
/*  40 */     String msg = message.trim();
/*     */ 
/*     */     
/*  43 */     for (String trigger : session.rule.sessionCancelTriggers) {
/*  44 */       if (msg.equalsIgnoreCase(trigger)) {
/*  45 */         cancelSession(player);
/*  46 */         return true;
/*     */       } 
/*     */     } 
/*     */ 
/*     */     
/*  51 */     for (String trigger : session.rule.sessionSkipTriggers) {
/*  52 */       if (msg.equalsIgnoreCase(trigger)) {
/*  53 */         ConfigManager.RuleConfig.SessionStep sessionStep = session.rule.sessionSteps.get(session.currentStep);
/*  54 */         if (sessionStep.defaultValue != null) {
/*  55 */           session.args.put(sessionStep.id, sessionStep.defaultValue);
/*  56 */           session.currentStep++;
/*  57 */           if (session.currentStep < session.rule.sessionSteps.size()) {
/*  58 */             askStep(player, session);
/*     */           } else {
/*  60 */             finishSession(player, session);
/*     */           } 
/*     */         } 
/*  63 */         return true;
/*     */       } 
/*     */     } 
/*     */ 
/*     */     
/*  68 */     String waitTime = parseWaitTime(msg);
/*  69 */     if (waitTime != null) {
/*  70 */       session.idleTimeout = parseWaitTimeMillis(msg);
/*  71 */       session.idleUntil = System.currentTimeMillis() + session.idleTimeout;
/*  72 */       this.plugin.getResponseHandler().executeAction(player, session.rule.sessionIdleMessage
/*  73 */           .replace("%session_idle_left%", waitTime));
/*  74 */       scheduleReminder(player, session);
/*  75 */       return true;
/*     */     } 
/*     */ 
/*     */     
/*  79 */     String mentioned = this.plugin.getExpansion().getTargetPlayerName(playerId);
/*  80 */     if (mentioned != null && !mentioned.equals(session.target)) {
/*  81 */       session.target = mentioned;
/*  82 */       this.plugin.getResponseHandler().executeAction(player, "message! &#EA7AFF▏ &rЦель изменена на &e" + mentioned + "&r. Повторяю вопрос:");
/*     */       
/*  84 */       askStep(player, session);
/*  85 */       return true;
/*     */     } 
/*     */ 
/*     */     
/*  89 */     ConfigManager.RuleConfig.SessionStep step = session.rule.sessionSteps.get(session.currentStep);
/*  90 */     if (msg.matches(step.validate)) {
/*  91 */       session.args.put(step.id, msg);
/*  92 */       session.currentStep++;
/*  93 */       cancelReminder(playerId);
/*  94 */       if (session.currentStep < session.rule.sessionSteps.size()) {
/*  95 */         askStep(player, session);
/*     */       } else {
/*  97 */         finishSession(player, session);
/*     */       } 
/*  99 */     } else if (step.errorMessage != null) {
/* 100 */       this.plugin.getResponseHandler().executeAction(player, step.errorMessage);
/*     */     } 
/* 102 */     return true;
/*     */   }
/*     */   
/*     */   private void askStep(Player player, SessionData session) {
/* 106 */     cancelReminder(player.getUniqueId());
/* 107 */     ConfigManager.RuleConfig.SessionStep step = session.rule.sessionSteps.get(session.currentStep);
/*     */ 
/*     */     
/* 110 */     String prompt = step.prompt.replace("%session_target%", session.target).replace("%session_current_step%", String.valueOf(session.currentStep + 1));
/* 111 */     this.plugin.getResponseHandler().executeAction(player, prompt);
/* 112 */     scheduleIdleTimeout(player, session);
/*     */   }
/*     */   
/*     */   private void scheduleIdleTimeout(Player player, SessionData session) {
/* 116 */     UUID playerId = player.getUniqueId();
/* 117 */     cancelReminder(playerId);
/*     */     
/* 119 */     if (session.rule.sessionIdleTimeout > 0) {
/* 120 */       session.idleUntil = System.currentTimeMillis() + session.rule.sessionIdleTimeout * 1000L;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */       
/* 127 */       int taskId = Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> { ConfigManager.RuleConfig.SessionStep step = session.rule.sessionSteps.get(session.currentStep); this.plugin.getResponseHandler().executeAction(player, step.prompt.replace("%session_target%", session.target).replace("%session_current_step%", String.valueOf(session.currentStep + 1))); }session.rule.sessionIdleTimeout * 20L).getTaskId();
/*     */       
/* 129 */       this.scheduledTasks.put(playerId, Integer.valueOf(taskId));
/*     */     } 
/*     */   }
/*     */   
/*     */   private void scheduleReminder(Player player, SessionData session) {
/* 134 */     UUID playerId = player.getUniqueId();
/* 135 */     cancelReminder(playerId);
/*     */     
/* 137 */     if (session.idleTimeout > 0L) {
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */       
/* 143 */       int taskId = Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> { ConfigManager.RuleConfig.SessionStep step = session.rule.sessionSteps.get(session.currentStep); this.plugin.getResponseHandler().executeAction(player, step.prompt.replace("%session_target%", session.target).replace("%session_current_step%", String.valueOf(session.currentStep + 1))); }session.idleTimeout / 50L).getTaskId();
/*     */       
/* 145 */       this.scheduledTasks.put(playerId, Integer.valueOf(taskId));
/*     */     } 
/*     */   }
/*     */   
/*     */   private void cancelReminder(UUID playerId) {
/* 150 */     Integer taskId = this.scheduledTasks.remove(playerId);
/* 151 */     if (taskId != null) {
/* 152 */       Bukkit.getScheduler().cancelTask(taskId.intValue());
/*     */     }
/*     */   }
/*     */   
/*     */   private void finishSession(Player player, SessionData session) {
/* 157 */     cancelReminder(player.getUniqueId());
/*     */     
/* 159 */     this.plugin.getExpansion().setArgs(player.getUniqueId(), session.args);
/* 160 */     this.plugin.getExpansion().setTargetPlayer(player.getUniqueId(), session.target);
/*     */     
/* 162 */     for (String answer : session.rule.answers) {
/* 163 */       this.plugin.getResponseHandler().executeAction(player, answer);
/*     */     }
/* 165 */     if (!session.rule.randomAnswers.isEmpty()) {
/* 166 */       String randomAnswer = session.rule.randomAnswers.get((new Random())
/* 167 */           .nextInt(session.rule.randomAnswers.size()));
/* 168 */       this.plugin.getResponseHandler().executeAction(player, randomAnswer);
/*     */     } 
/*     */ 
/*     */     
/* 172 */     session.finished = true;
/* 173 */     int argsTimeout = (session.rule.sessionArgsTimeout > 0) ? session.rule.sessionArgsTimeout : 1200;
/* 174 */     session.argsKeepUntil = System.currentTimeMillis() + argsTimeout * 50L;
/* 175 */     Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.sessions.remove(player.getUniqueId()), argsTimeout);
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public void cancelSession(Player player) {
/* 181 */     UUID playerId = player.getUniqueId();
/* 182 */     SessionData session = this.sessions.remove(playerId);
/* 183 */     cancelReminder(playerId);
/* 184 */     if (session != null && session.rule.sessionCancelMessage != null) {
/* 185 */       this.plugin.getResponseHandler().executeAction(player, session.rule.sessionCancelMessage);
/*     */     }
/*     */   }
/*     */   
/*     */   public boolean isInSession(UUID playerId) {
/* 190 */     return (getSession(playerId) != null && !(getSession(playerId)).finished);
/*     */   }
/*     */   
/*     */   public SessionData getSession(UUID playerId) {
/* 194 */     SessionData session = this.sessions.get(playerId);
/* 195 */     if (session == null) return null; 
/* 196 */     if (session.finished && System.currentTimeMillis() > session.argsKeepUntil) {
/* 197 */       this.sessions.remove(playerId);
/* 198 */       return null;
/*     */     } 
/* 200 */     return session;
/*     */   }
/*     */   
/*     */   public void clearAll() {
/* 204 */     Objects.requireNonNull(Bukkit.getScheduler()); this.scheduledTasks.values().forEach(Bukkit.getScheduler()::cancelTask);
/* 205 */     this.scheduledTasks.clear();
/* 206 */     this.sessions.clear();
/*     */   }
/*     */   
/*     */   private String parseWaitTime(String message) {
/* 210 */     String lower = message.toLowerCase();
/* 211 */     if (lower.matches(".*подожди.*\\d+.*(минут|мин|секунд|сек|час|часов).*") || lower
/* 212 */       .matches(".*wait.*\\d+.*(min|sec|hour|minute|second).*")) {
/* 213 */       Pattern p = Pattern.compile("(\\d+)\\s*(минут|мин|секунд|сек|час|часов|min|sec|hour|minute|second)");
/* 214 */       Matcher m = p.matcher(lower);
/* 215 */       if (m.find()) {
/* 216 */         return m.group(1) + " " + m.group(1);
/*     */       }
/*     */     } 
/* 219 */     return null;
/*     */   }
/*     */   
/*     */   private long parseWaitTimeMillis(String message) {
/* 223 */     String lower = message.toLowerCase();
/* 224 */     Pattern p = Pattern.compile("(\\d+)\\s*(минут|мин|секунд|сек|час|часов|min|sec|hour|minute|second)");
/* 225 */     Matcher m = p.matcher(lower);
/* 226 */     if (m.find()) {
/* 227 */       int num = Integer.parseInt(m.group(1));
/* 228 */       String unit = m.group(2);
/* 229 */       if (unit.matches("секунд|сек|sec|second")) return num * 1000L; 
/* 230 */       if (unit.matches("минут|мин|min|minute")) return num * 60000L; 
/* 231 */       if (unit.matches("час|часов|hour")) return num * 3600000L; 
/*     */     } 
/* 233 */     return 30000L;
/*     */   }
/*     */   
/*     */   public static class SessionData {
/*     */     public ConfigManager.RuleConfig rule;
/*     */     public String chatId;
/*     */     public String listenChat;
/*     */     public String target;
/* 241 */     public int currentStep = 0;
/* 242 */     public Map<String, String> args = new HashMap<>();
/* 243 */     public long idleTimeout = 0L;
/* 244 */     public long idleUntil = 0L;
/*     */     public boolean finished = false;
/* 246 */     public long argsKeepUntil = 0L;
/*     */     
/*     */     public SessionData(ConfigManager.RuleConfig rule, String chatId, String target) {
/* 249 */       this.rule = rule;
/* 250 */       this.chatId = chatId;
/* 251 */       this.listenChat = (rule.sessionListenChat != null) ? rule.sessionListenChat : "same";
/* 252 */       this.target = target;
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\LQDFIRE\Desktop\QQAssistant-1.0.0.jar!\com\allfire\qqassist\chat\SessionManager.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */