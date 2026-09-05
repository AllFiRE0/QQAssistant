/*     */ package com.allfire.qqassist.expansion;
/*     */ import com.allfire.qqassist.QQAssistPlugin;
/*     */ import com.allfire.qqassist.chat.SessionManager;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collection;
/*     */ import java.util.Collections;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Random;
/*     */ import java.util.UUID;
/*     */ import java.util.concurrent.ConcurrentHashMap;
/*     */ import me.clip.placeholderapi.PlaceholderAPI;
/*     */ import org.bukkit.Bukkit;
/*     */ import org.bukkit.OfflinePlayer;
/*     */ import org.bukkit.entity.Player;
/*     */ 
/*     */ public class QQAssistExpansion extends PlaceholderExpansion {
/*  18 */   private final Map<UUID, String> targetPlayers = new ConcurrentHashMap<>(); private final QQAssistPlugin plugin;
/*  19 */   private final Map<UUID, Long> lastTargetTime = new ConcurrentHashMap<>();
/*  20 */   private final Map<UUID, Map<Integer, String>> argValuesMap = new ConcurrentHashMap<>();
/*  21 */   private final Map<UUID, Map<String, String>> playerArgs = new ConcurrentHashMap<>();
/*  22 */   private final Map<UUID, String> lastMessages = new ConcurrentHashMap<>();
/*  23 */   private final Random random = new Random();
/*     */   private long sessionTimeout;
/*     */   private String defaultTargetName;
/*     */   private String defaultParse;
/*     */   private String defaultData;
/*     */   
/*     */   public QQAssistExpansion(QQAssistPlugin plugin) {
/*  30 */     this.plugin = plugin;
/*  31 */     loadDefaults();
/*     */   }
/*     */   
/*     */   private void loadDefaults() {
/*  35 */     this.sessionTimeout = this.plugin.getConfig().getLong("settings.session-timeout", 60L) * 1000L;
/*  36 */     this.defaultTargetName = this.plugin.getConfig().getString("settings.placeholder-defaults.target-name", "кто-то");
/*     */     
/*  38 */     this.defaultParse = this.plugin.getConfig().getString("settings.placeholder-defaults.target-parse", "не нашлось");
/*     */     
/*  40 */     this.defaultData = this.plugin.getConfig().getString("settings.placeholder-defaults.target-data", "—");
/*     */   }
/*     */ 
/*     */   
/*     */   public String getAuthor() {
/*  45 */     return "AllF1RE";
/*     */   } public String getIdentifier() {
/*  47 */     return "qqassist";
/*     */   } public String getVersion() {
/*  49 */     return "1.0.0";
/*     */   }
/*     */   
/*     */   public List<String> getPlaceholders() {
/*  53 */     List<String> placeholders = new ArrayList<>();
/*  54 */     placeholders.add("%qqassist_prefix%");
/*  55 */     placeholders.add("%qqassist_message%");
/*  56 */     placeholders.add("%qqassist_target%");
/*  57 */     placeholders.add("%qqassist_random_target%");
/*  58 */     placeholders.add("%qqassist_random_online_target%");
/*  59 */     placeholders.add("%qqassist_session_target%");
/*  60 */     placeholders.add("%qqassist_session_arg_<название>%");
/*  61 */     placeholders.add("%qqassist_session_idle_left%");
/*  62 */     placeholders.add("%qqassist_session_current_step%");
/*  63 */     placeholders.add("%qqassist_arg_<номер>%");
/*  64 */     placeholders.add("%qqassist_parse_{placeholder}%");
/*  65 */     placeholders.add("%qqassist_target_uuid%");
/*  66 */     placeholders.add("%qqassist_target_world%");
/*  67 */     placeholders.add("%qqassist_target_health%");
/*  68 */     placeholders.add("%qqassist_target_max_health%");
/*  69 */     placeholders.add("%qqassist_target_level%");
/*  70 */     placeholders.add("%qqassist_target_gamemode%");
/*  71 */     placeholders.add("%qqassist_target_food%");
/*  72 */     placeholders.add("%qqassist_target_xp%");
/*  73 */     return placeholders;
/*     */   }
/*     */   
/*     */   public void setTargetPlayer(UUID playerId, String targetName) {
/*  77 */     this.targetPlayers.put(playerId, targetName);
/*  78 */     this.lastTargetTime.put(playerId, Long.valueOf(System.currentTimeMillis()));
/*     */   }
/*     */   
/*     */   public void clearTargetPlayer(UUID playerId) {
/*  82 */     this.targetPlayers.remove(playerId);
/*  83 */     this.lastTargetTime.remove(playerId);
/*     */   }
/*     */   
/*     */   public void setArgValues(UUID playerId, Map<Integer, String> values) {
/*  87 */     this.argValuesMap.put(playerId, values);
/*     */   }
/*     */   
/*     */   public void setArgs(UUID playerId, Map<String, String> args) {
/*  91 */     if (args != null && !args.isEmpty()) {
/*  92 */       this.playerArgs.put(playerId, args);
/*     */     }
/*     */   }
/*     */   
/*     */   public void setLastMessage(UUID playerId, String message) {
/*  97 */     this.lastMessages.put(playerId, message);
/*     */   }
/*     */   
/*     */   public Map<Integer, String> getArgValues(UUID playerId) {
/* 101 */     return this.argValuesMap.getOrDefault(playerId, Collections.emptyMap());
/*     */   }
/*     */   
/*     */   public String getTargetPlayerName(UUID playerId) {
/* 105 */     Long lastTime = this.lastTargetTime.get(playerId);
/* 106 */     if (lastTime != null && System.currentTimeMillis() - lastTime.longValue() > this.sessionTimeout) {
/* 107 */       this.targetPlayers.remove(playerId);
/* 108 */       this.lastTargetTime.remove(playerId);
/* 109 */       return null;
/*     */     } 
/* 111 */     return this.targetPlayers.get(playerId);
/*     */   }
/*     */   
/*     */   public Player getTargetPlayer(UUID playerId) {
/* 115 */     String name = getTargetPlayerName(playerId);
/* 116 */     return (name != null) ? Bukkit.getPlayer(name) : null;
/*     */   }
/*     */ 
/*     */   
/*     */   public String onRequest(OfflinePlayer p, String params) {
/* 121 */     loadDefaults();
/*     */     
/* 123 */     if (params.equals("prefix")) {
/* 124 */       return this.plugin.getConfigManager().getPrefix();
/*     */     }
/*     */     
/* 127 */     if (params.equals("message")) {
/* 128 */       if (p == null) return ""; 
/* 129 */       return this.lastMessages.getOrDefault(p.getUniqueId(), "");
/*     */     } 
/*     */     
/* 132 */     if (params.equals("random_target")) {
/* 133 */       OfflinePlayer[] allPlayers = Bukkit.getOfflinePlayers();
/* 134 */       List<String> validNames = new ArrayList<>();
/* 135 */       for (OfflinePlayer op : allPlayers) {
/* 136 */         if (op.hasPlayedBefore() && op.getName() != null) {
/* 137 */           validNames.add(op.getName());
/*     */         }
/*     */       } 
/* 140 */       if (validNames.isEmpty()) return this.defaultTargetName; 
/* 141 */       return validNames.get(this.random.nextInt(validNames.size()));
/*     */     } 
/*     */     
/* 144 */     if (params.equals("random_online_target")) {
/* 145 */       Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
/* 146 */       if (onlinePlayers.isEmpty()) return this.defaultTargetName; 
/* 147 */       List<Player> list = new ArrayList<>(onlinePlayers);
/* 148 */       return ((Player)list.get(this.random.nextInt(list.size()))).getName();
/*     */     } 
/*     */ 
/*     */     
/* 152 */     if (params.startsWith("arg_")) {
/* 153 */       if (p == null) return ""; 
/*     */       try {
/* 155 */         int num = Integer.parseInt(params.substring(4));
/* 156 */         return getArgValues(p.getUniqueId()).getOrDefault(Integer.valueOf(num), "");
/* 157 */       } catch (NumberFormatException e) {
/* 158 */         return "";
/*     */       } 
/*     */     } 
/*     */ 
/*     */     
/* 163 */     if (params.startsWith("session_")) {
/* 164 */       if (p == null) return ""; 
/* 165 */       UUID uUID = p.getUniqueId();
/* 166 */       SessionManager.SessionData session = this.plugin.getSessionManager().getSession(uUID);
/* 167 */       if (session == null) return "";
/*     */       
/* 169 */       if (params.equals("session_target")) return session.target; 
/* 170 */       if (params.equals("session_current_step")) return String.valueOf(session.currentStep + 1); 
/* 171 */       if (params.equals("session_idle_left")) {
/* 172 */         long left = session.idleUntil - System.currentTimeMillis();
/* 173 */         if (left <= 0L) return "0 секунд"; 
/* 174 */         if (left >= 3600000L) return "" + left / 3600000L + " часов"; 
/* 175 */         if (left >= 60000L) return "" + left / 60000L + " минут"; 
/* 176 */         return "" + left / 1000L + " секунд";
/*     */       } 
/* 178 */       if (params.startsWith("session_arg_")) {
/* 179 */         return (String)session.args.getOrDefault(params.substring(12), "");
/*     */       }
/* 181 */       return "";
/*     */     } 
/*     */     
/* 184 */     if (p == null || !p.isOnline()) return ""; 
/* 185 */     Player viewer = p.getPlayer();
/* 186 */     if (viewer == null) return ""; 
/* 187 */     UUID viewerId = viewer.getUniqueId();
/* 188 */     String targetName = getTargetPlayerName(viewerId);
/*     */     
/* 190 */     if (params.equals("target")) {
/* 191 */       if (targetName != null && !targetName.isEmpty()) return targetName; 
/* 192 */       return viewer.getName();
/*     */     } 
/*     */     
/* 195 */     Player target = getTargetPlayer(viewerId);
/*     */     
/* 197 */     if (params.startsWith("parse_")) {
/* 198 */       String result, placeholderName = params.substring(6);
/* 199 */       if (targetName == null || targetName.isEmpty()) {
/* 200 */         result = PlaceholderAPI.setPlaceholders(viewer, "%" + placeholderName + "%");
/* 201 */         if (result == null || result.isEmpty() || result.equals("%" + placeholderName + "%")) return this.defaultParse; 
/* 202 */         return result;
/*     */       } 
/*     */       
/* 205 */       if (target != null) {
/* 206 */         result = PlaceholderAPI.setPlaceholders(target, "%" + placeholderName + "%");
/*     */       } else {
/* 208 */         OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
/* 209 */         if (offlineTarget == null || !offlineTarget.hasPlayedBefore()) return this.defaultParse; 
/* 210 */         result = PlaceholderAPI.setPlaceholders(offlineTarget, "%" + placeholderName + "%");
/*     */       } 
/* 212 */       if (result == null || result.isEmpty() || result.equals("%" + placeholderName + "%")) return this.defaultParse; 
/* 213 */       return result;
/*     */     } 
/*     */     
/* 216 */     if (params.startsWith("target_")) {
/* 217 */       if (target == null) return this.defaultData; 
/* 218 */       String dataType = params.substring(7);
/* 219 */       switch (dataType.toLowerCase()) { case "uuid":
/* 220 */           return target.getUniqueId().toString();
/* 221 */         case "world": return target.getWorld().getName();
/* 222 */         case "health": return String.format("%.1f", new Object[] { Double.valueOf(target.getHealth()) });
/* 223 */         case "max_health": return String.format("%.1f", new Object[] { Double.valueOf(target.getMaxHealth()) });
/* 224 */         case "level": return String.valueOf(target.getLevel());
/* 225 */         case "gamemode": return target.getGameMode().name();
/* 226 */         case "food": return String.valueOf(target.getFoodLevel());
/* 227 */         case "xp": return String.valueOf(target.getTotalExperience()); }
/* 228 */        return this.defaultData;
/*     */     } 
/*     */ 
/*     */     
/* 232 */     return null;
/*     */   }
/*     */   
/*     */   public boolean persist() {
/* 236 */     return true;
/*     */   } public boolean canRegister() {
/* 238 */     return true;
/*     */   }
/*     */ }


/* Location:              C:\Users\LQDFIRE\Desktop\QQAssistant-1.0.0.jar!\com\allfire\qqassist\expansion\QQAssistExpansion.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */