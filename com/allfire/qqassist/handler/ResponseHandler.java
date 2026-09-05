/*     */ package com.allfire.qqassist.handler;
/*     */ import com.allfire.qqassist.QQAssistPlugin;
/*     */ import com.allfire.qqassist.config.ConfigManager;
/*     */ import java.util.HashMap;
/*     */ import java.util.Map;
/*     */ import java.util.Random;
/*     */ import java.util.UUID;
/*     */ import java.util.regex.Pattern;
/*     */ import me.clip.placeholderapi.PlaceholderAPI;
/*     */ import net.kyori.adventure.text.Component;
/*     */ import net.kyori.adventure.text.TextComponent;
/*     */ import net.kyori.adventure.text.minimessage.MiniMessage;
/*     */ import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
/*     */ import net.kyori.adventure.title.Title;
/*     */ import net.kyori.adventure.util.Ticks;
/*     */ import org.bukkit.Bukkit;
/*     */ import org.bukkit.Sound;
/*     */ import org.bukkit.command.CommandSender;
/*     */ import org.bukkit.entity.Player;
/*     */ import org.bukkit.plugin.Plugin;
/*     */ 
/*     */ public class ResponseHandler {
/*  23 */   private final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>(); private final QQAssistPlugin plugin;
/*  24 */   private final Map<UUID, Long> playerLastQuestion = new HashMap<>();
/*  25 */   private final Random random = new Random();
/*     */   
/*     */   public ResponseHandler(QQAssistPlugin plugin) {
/*  28 */     this.plugin = plugin;
/*     */   }
/*     */   
/*     */   public void processMessage(Player player, String message, String chatId) {
/*  32 */     UUID playerId = player.getUniqueId();
/*  33 */     Long lastQuestion = this.playerLastQuestion.get(playerId);
/*  34 */     if (lastQuestion != null && System.currentTimeMillis() - lastQuestion.longValue() < 500L) {
/*     */       return;
/*     */     }
/*  37 */     this.playerLastQuestion.put(playerId, Long.valueOf(System.currentTimeMillis()));
/*     */     
/*  39 */     Map<String, ConfigManager.RuleConfig> rules = this.plugin.getConfigManager().getRules();
/*     */     
/*  41 */     for (Map.Entry<String, ConfigManager.RuleConfig> entry : rules.entrySet()) {
/*  42 */       ConfigManager.RuleConfig rule = entry.getValue();
/*     */       
/*  44 */       if (!rule.allowedChats.isEmpty() && !rule.allowedChats.contains(chatId)) {
/*     */         continue;
/*     */       }
/*  47 */       if (!rule.permission.isEmpty() && !player.hasPermission(rule.permission)) {
/*     */         continue;
/*     */       }
/*  50 */       if (isOnCooldown(playerId, rule.name, rule.cooldownTicks)) {
/*     */         continue;
/*     */       }
/*     */       
/*  54 */       if (matchesQuestion(message, rule)) {
/*     */         
/*  56 */         Map<Integer, String> argValues = new HashMap<>();
/*  57 */         if (rule.argsDef != null && !rule.argsDef.isEmpty()) {
/*  58 */           String lowerMessage = message.toLowerCase();
/*  59 */           for (int i = 0; i < rule.argsDef.size(); i++) {
/*  60 */             ConfigManager.RuleConfig.ArgDef argDef = rule.argsDef.get(i);
/*  61 */             String foundValue = argDef.defaultValue;
/*  62 */             for (String val : argDef.values) {
/*  63 */               if (lowerMessage.contains(val.toLowerCase())) {
/*  64 */                 foundValue = val;
/*     */                 break;
/*     */               } 
/*     */             } 
/*  68 */             argValues.put(Integer.valueOf(i + 1), foundValue);
/*     */           } 
/*  70 */           this.plugin.getExpansion().setArgValues(player.getUniqueId(), argValues);
/*     */         } 
/*     */ 
/*     */         
/*  74 */         if (rule.sessionEnabled && !rule.sessionSteps.isEmpty()) {
/*  75 */           String target = this.plugin.getExpansion().getTargetPlayerName(player.getUniqueId());
/*  76 */           if (target == null) target = player.getName(); 
/*  77 */           this.plugin.getSessionManager().startSession(player, rule, chatId, target);
/*  78 */           setCooldown(player.getUniqueId(), rule.name);
/*     */           
/*     */           break;
/*     */         } 
/*  82 */         if (rule.chance < 100 && this.random.nextInt(100) >= rule.chance) {
/*     */           continue;
/*     */         }
/*     */         
/*  86 */         int delayTicks = Math.max(rule.delayTicks, 0);
/*  87 */         if (delayTicks > 0) {
/*  88 */           Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> executeResponses(player, rule), delayTicks);
/*     */         } else {
/*  90 */           executeResponses(player, rule);
/*     */         } 
/*     */         
/*  93 */         setCooldown(playerId, rule.name);
/*     */         break;
/*     */       } 
/*     */     } 
/*     */   }
/*     */   
/*     */   private boolean matchesQuestion(String message, ConfigManager.RuleConfig rule) {
/* 100 */     String lowerMessage = message.toLowerCase().trim();
/* 101 */     for (String exact : rule.questionsExact) {
/* 102 */       if (lowerMessage.equals(exact.toLowerCase().trim())) return true; 
/*     */     } 
/* 104 */     for (String contains : rule.questionsContains) {
/* 105 */       if (lowerMessage.contains(contains.toLowerCase().trim())) return true; 
/*     */     } 
/* 107 */     for (Pattern pattern : rule.questionsRegex) {
/* 108 */       if (pattern.matcher(message).find()) return true; 
/*     */     } 
/* 110 */     return false;
/*     */   }
/*     */   
/*     */   private void executeResponses(Player player, ConfigManager.RuleConfig rule) {
/* 114 */     for (String answer : rule.answers) {
/* 115 */       if (answer == null || answer.isEmpty())
/* 116 */         continue;  executeAction(player, answer);
/*     */     } 
/* 118 */     if (!rule.randomAnswers.isEmpty()) {
/* 119 */       String randomAnswer = rule.randomAnswers.get(this.random.nextInt(rule.randomAnswers.size()));
/* 120 */       executeAction(player, randomAnswer);
/*     */     } 
/*     */   }
/*     */   
/*     */   public void executeAction(Player player, String action) {
/* 125 */     if (action == null || action.isEmpty())
/*     */       return; 
/*     */     try {
/* 128 */       action = action.replace("%prefix%", this.plugin.getConfigManager().getPrefix());
/*     */ 
/*     */       
/* 131 */       if (action.startsWith("arg:")) {
/* 132 */         handleArgAction(player, action);
/*     */         
/*     */         return;
/*     */       } 
/*     */       
/* 137 */       if (action.startsWith("delay:")) {
/* 138 */         handleDelayAction(player, action);
/*     */         
/*     */         return;
/*     */       } 
/*     */       
/* 143 */       if (action.contains("check:[")) {
/* 144 */         handleCheckAction(player, action);
/*     */         
/*     */         return;
/*     */       } 
/*     */       
/* 149 */       if (action.startsWith("sound!")) {
/* 150 */         handleSound(player, action.substring(6).trim());
/* 151 */       } else if (action.startsWith("gSound!")) {
/* 152 */         handleGlobalSound(player, action.substring(7).trim());
/* 153 */       } else if (action.startsWith("asConsole!")) {
/* 154 */         handleConsoleCommand(player, action.substring(10).trim());
/* 155 */       } else if (action.startsWith("asPlayer!")) {
/* 156 */         handlePlayerCommand(player, action.substring(9).trim());
/* 157 */       } else if (action.startsWith("gMessage!")) {
/* 158 */         handleGlobalMessage(player, action.substring(9).trim());
/* 159 */       } else if (action.startsWith("message!")) {
/* 160 */         handleMessage(player, action.substring(8).trim());
/* 161 */       } else if (action.startsWith("actionbar:")) {
/* 162 */         handleActionbarWithDuration(player, action);
/* 163 */       } else if (action.startsWith("actionbar!")) {
/* 164 */         handleActionbar(player, action.substring(10).trim(), 60);
/* 165 */       } else if (action.startsWith("title:")) {
/* 166 */         handleTitleWithTimings(player, action);
/* 167 */       } else if (action.startsWith("title!")) {
/* 168 */         handleTitle(player, action.substring(6).trim(), 20, 40, 20);
/*     */       } 
/* 170 */     } catch (Exception e) {
/* 171 */       this.plugin.getLogger().warning("Ошибка при выполнении действия: " + action);
/*     */     } 
/*     */   }
/*     */   
/*     */   private void handleArgAction(Player player, String action) {
/* 176 */     String remaining = action;
/* 177 */     Map<Integer, String> argValues = this.plugin.getExpansion().getArgValues(player.getUniqueId());
/*     */     
/* 179 */     while (remaining.startsWith("arg:")) {
/* 180 */       int bracketStart = remaining.indexOf("[");
/* 181 */       int bracketEnd = remaining.indexOf("]!");
/* 182 */       if (bracketStart == -1 || bracketEnd == -1)
/*     */         return; 
/*     */       try {
/* 185 */         String numStr = remaining.substring(4, bracketStart).trim();
/* 186 */         if (numStr.endsWith(":")) numStr = numStr.substring(0, numStr.length() - 1); 
/* 187 */         int argNum = Integer.parseInt(numStr);
/* 188 */         String valuesStr = remaining.substring(bracketStart + 1, bracketEnd);
/*     */         
/* 190 */         String actualValue = argValues.getOrDefault(Integer.valueOf(argNum), "");
/* 191 */         if (!checkArgMatch(player, valuesStr, actualValue))
/*     */           return; 
/* 193 */         remaining = remaining.substring(bracketEnd + 2).trim();
/* 194 */       } catch (NumberFormatException e) {
/*     */         return;
/*     */       } 
/*     */     } 
/*     */     
/* 199 */     if (!remaining.isEmpty()) {
/* 200 */       executeAction(player, remaining);
/*     */     }
/*     */   }
/*     */   
/*     */   private void handleDelayAction(Player player, String action) {
/*     */     try {
/* 206 */       String[] parts = action.substring(6).split("!", 2);
/* 207 */       String delayStr = parts[0].trim();
/* 208 */       int delayTicks = Integer.parseInt(delayStr);
/*     */       
/* 210 */       if (parts.length > 1) {
/* 211 */         String delayedAction = parts[1].trim();
/* 212 */         Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> executeAction(player, delayedAction), delayTicks);
/*     */       } 
/* 214 */     } catch (NumberFormatException e) {
/* 215 */       this.plugin.getLogger().warning("Ошибка парсинга задержки в действии: " + action);
/*     */     } 
/*     */   }
/*     */   
/*     */   private void handleCheckAction(Player player, String action) {
/* 220 */     int bracketStart = action.indexOf("check:[") + 7;
/* 221 */     int bracketEnd = action.indexOf("]!", bracketStart);
/* 222 */     if (bracketEnd == -1) {
/* 223 */       this.plugin.getLogger().warning("Неверный формат check действия: " + action);
/*     */       
/*     */       return;
/*     */     } 
/* 227 */     String condition = action.substring(bracketStart, bracketEnd);
/* 228 */     String restAction = action.substring(bracketEnd + 2).trim();
/*     */     
/* 230 */     if (evaluateCondition(player, condition)) {
/* 231 */       executeAction(player, restAction);
/*     */     }
/*     */   }
/*     */   
/*     */   private void handleActionbarWithDuration(Player player, String action) {
/*     */     try {
/* 237 */       String[] parts = action.substring(10).split("!", 2);
/* 238 */       String durationStr = parts[0].trim();
/* 239 */       int duration = Integer.parseInt(durationStr);
/*     */       
/* 241 */       if (parts.length > 1) {
/* 242 */         handleActionbar(player, parts[1].trim(), duration);
/*     */       }
/* 244 */     } catch (NumberFormatException e) {
/* 245 */       this.plugin.getLogger().warning("Ошибка парсинга длительности actionbar: " + action);
/*     */     } 
/*     */   }
/*     */   
/*     */   private void handleTitleWithTimings(Player player, String action) {
/*     */     try {
/* 251 */       String[] parts = action.substring(6).split("!", 2);
/* 252 */       if (parts.length > 1) {
/* 253 */         String[] times = parts[0].split(":");
/* 254 */         if (times.length < 3) {
/* 255 */           this.plugin.getLogger().warning("Неверный формат title: требуется 3 параметра времени (fadeIn:stay:fadeOut)");
/*     */           
/*     */           return;
/*     */         } 
/* 259 */         int fadeIn = Integer.parseInt(times[0].trim());
/* 260 */         int stay = Integer.parseInt(times[1].trim());
/* 261 */         int fadeOut = Integer.parseInt(times[2].trim());
/* 262 */         handleTitle(player, parts[1].trim(), fadeIn, stay, fadeOut);
/*     */       } 
/* 264 */     } catch (NumberFormatException e) {
/* 265 */       this.plugin.getLogger().warning("Ошибка парсинга параметров title: " + action);
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   private boolean checkArgMatch(Player player, String expected, String actual) {
/* 271 */     if (expected.equals("*")) return true; 
/* 272 */     if (expected.startsWith("regex:")) {
/*     */       try {
/* 274 */         return actual.matches(expected.substring(6));
/* 275 */       } catch (Exception e) {
/* 276 */         this.plugin.getLogger().warning("Ошибка в regex: " + expected);
/* 277 */         return false;
/*     */       } 
/*     */     }
/* 280 */     if (expected.startsWith("papi:")) {
/* 281 */       String papiValue = PlaceholderAPI.setPlaceholders(player, expected.substring(5));
/* 282 */       return actual.equalsIgnoreCase(papiValue);
/*     */     } 
/* 284 */     if (expected.startsWith("check:")) {
/* 285 */       return evaluateCondition(player, expected.substring(6));
/*     */     }
/* 287 */     if (expected.startsWith("contains:")) {
/* 288 */       return actual.toLowerCase().contains(expected.substring(9).toLowerCase());
/*     */     }
/* 290 */     if (expected.contains("||")) {
/* 291 */       for (String val : expected.split("\\|\\|")) {
/* 292 */         if (val.trim().equalsIgnoreCase(actual)) return true; 
/*     */       } 
/* 294 */       return false;
/*     */     } 
/* 296 */     return expected.equalsIgnoreCase(actual);
/*     */   }
/*     */   
/*     */   private Component parseMessage(String message) {
/* 300 */     message = message.replaceAll("\\{#([A-Fa-f0-9]{6})>\\}(.*?)\\{#([A-Fa-f0-9]{6})<\\}", "<gradient:#$1:#$3>$2</gradient>");
/*     */     
/* 302 */     if (message.contains("<gradient:") || message.contains("<#") || message.contains("<color:")) {
/*     */       try {
/* 304 */         return MiniMessage.miniMessage().deserialize(message);
/* 305 */       } catch (Exception exception) {}
/*     */     }
/* 307 */     return (Component)LegacyComponentSerializer.legacyAmpersand().deserialize(message);
/*     */   }
/*     */   
/*     */   private String formatForCommand(String text, Player player) {
/* 311 */     text = LegacyComponentSerializer.legacyAmpersand().serialize(parseMessage(text));
/* 312 */     return PlaceholderAPI.setPlaceholders(player, text);
/*     */   }
/*     */   
/*     */   private Component formatForDisplay(String text, Player player) {
/* 316 */     text = PlaceholderAPI.setPlaceholders(player, text);
/* 317 */     return parseMessage(text);
/*     */   }
/*     */   
/*     */   private void handleSound(Player player, String soundParams) {
/*     */     try {
/* 322 */       String[] parts = soundParams.split(" ");
/* 323 */       if (parts.length < 3) {
/* 324 */         this.plugin.getLogger().warning("Неверный формат звука: требуется НАЗВАНИЕ ГРОМКОСТЬ ТОН");
/*     */         return;
/*     */       } 
/* 327 */       Sound sound = Sound.valueOf(parts[0]);
/* 328 */       float volume = Float.parseFloat(parts[1]);
/* 329 */       float pitch = Float.parseFloat(parts[2]);
/* 330 */       player.playSound(player.getLocation(), sound, volume, pitch);
/* 331 */     } catch (IllegalArgumentException e) {
/* 332 */       this.plugin.getLogger().warning("Неизвестный звук: " + soundParams);
/* 333 */     } catch (Exception e) {
/* 334 */       this.plugin.getLogger().warning("Ошибка воспроизведения звука: " + soundParams);
/*     */     } 
/*     */   }
/*     */   
/*     */   private void handleGlobalSound(Player player, String soundParams) {
/*     */     try {
/* 340 */       String[] parts = soundParams.split(" ");
/* 341 */       if (parts.length < 3) {
/* 342 */         this.plugin.getLogger().warning("Неверный формат глобального звука: требуется НАЗВАНИЕ ГРОМКОСТЬ ТОН");
/*     */         return;
/*     */       } 
/* 345 */       Sound sound = Sound.valueOf(parts[0]);
/* 346 */       float volume = Float.parseFloat(parts[1]);
/* 347 */       float pitch = Float.parseFloat(parts[2]);
/* 348 */       for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
/* 349 */         onlinePlayer.playSound(onlinePlayer.getLocation(), sound, volume, pitch);
/*     */       }
/* 351 */     } catch (IllegalArgumentException e) {
/* 352 */       this.plugin.getLogger().warning("Неизвестный звук: " + soundParams);
/* 353 */     } catch (Exception e) {
/* 354 */       this.plugin.getLogger().warning("Ошибка воспроизведения глобального звука: " + soundParams);
/*     */     } 
/*     */   }
/*     */   
/*     */   private void handleConsoleCommand(Player player, String command) {
/* 359 */     command = formatForCommand(command, player);
/* 360 */     String finalCommand = command;
/* 361 */     Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), finalCommand));
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private void handlePlayerCommand(Player player, String command) {
/* 367 */     command = formatForCommand(command, player);
/* 368 */     String finalCommand = command;
/* 369 */     Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> player.performCommand(finalCommand));
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private void handleGlobalMessage(Player player, String message) {
/* 375 */     Bukkit.broadcast(formatForDisplay(message, player));
/*     */   }
/*     */   
/*     */   private void handleMessage(Player player, String message) {
/* 379 */     player.sendMessage(formatForDisplay(message, player));
/*     */   }
/*     */   
/*     */   private void handleActionbar(Player player, String message, int duration) {
/* 383 */     Component component = formatForDisplay(message, player);
/* 384 */     player.sendActionBar(component);
/* 385 */     int refreshTicks = this.plugin.getConfig().getInt("settings.actionbar-refresh-ticks", 20);
/* 386 */     if (duration > refreshTicks && refreshTicks > 0) {
/* 387 */       int repeats = duration / refreshTicks;
/* 388 */       for (int i = 1; i <= repeats; i++) {
/* 389 */         Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> player.sendActionBar(component), i * refreshTicks);
/*     */       }
/*     */     } 
/*     */   }
/*     */   
/*     */   private void handleTitle(Player player, String titleText, int fadeIn, int stay, int fadeOut) {
/*     */     Component title, component1;
/* 396 */     String text = PlaceholderAPI.setPlaceholders(player, titleText);
/*     */     
/* 398 */     TextComponent textComponent = Component.empty();
/* 399 */     if (text.contains("\\n")) {
/* 400 */       String[] parts = text.split("\\\\n", 2);
/* 401 */       title = parseMessage(parts[0]);
/* 402 */       component1 = parseMessage(parts[1]);
/*     */     } else {
/* 404 */       title = parseMessage(text);
/*     */     } 
/* 406 */     Title.Times times = Title.Times.times(
/* 407 */         Ticks.duration(fadeIn), 
/* 408 */         Ticks.duration(stay), 
/* 409 */         Ticks.duration(fadeOut));
/*     */     
/* 411 */     player.showTitle(Title.title(title, component1, times));
/*     */   }
/*     */   
/*     */   private boolean isOnCooldown(UUID playerId, String ruleName, int cooldownTicks) {
/* 415 */     Map<String, Long> cooldowns = this.playerCooldowns.get(playerId);
/* 416 */     if (cooldowns == null) return false; 
/* 417 */     Long cooldownUntil = cooldowns.get(ruleName);
/* 418 */     if (cooldownUntil == null) return false; 
/* 419 */     return (System.currentTimeMillis() < cooldownUntil.longValue());
/*     */   }
/*     */   
/*     */   private void setCooldown(UUID playerId, String ruleName) {
/* 423 */     ((Map<String, Long>)this.playerCooldowns
/* 424 */       .computeIfAbsent(playerId, k -> new HashMap<>()))
/* 425 */       .put(ruleName, Long.valueOf(System.currentTimeMillis() + 1000L));
/*     */   }
/*     */   
/*     */   private boolean evaluateCondition(Player player, String condition) {
/* 429 */     condition = PlaceholderAPI.setPlaceholders(player, condition);
/* 430 */     String[] ops = { ">=", "<=", "!=", "!<-", "!|-", "!-|", "<-", "|-", "-|", ">", "<", "=" };
/* 431 */     for (String op : ops) {
/* 432 */       int idx = condition.indexOf(op);
/* 433 */       if (idx != -1) {
/* 434 */         String left = condition.substring(0, idx).trim();
/* 435 */         String right = condition.substring(idx + op.length()).trim();
/*     */         try {
/* 437 */           double leftNum = Double.parseDouble(left);
/* 438 */           double rightNum = Double.parseDouble(right);
/* 439 */           switch (op) { case ">=":
/* 440 */               return (leftNum >= rightNum);
/* 441 */             case ">": return (leftNum > rightNum);
/* 442 */             case "<=": return (leftNum <= rightNum);
/* 443 */             case "<": return (leftNum < rightNum);
/* 444 */             case "=": return left.equals(right);
/* 445 */             case "!=": return !left.equals(right); }
/* 446 */            return false;
/*     */         }
/* 448 */         catch (NumberFormatException e) {
/* 449 */           switch (op) { case "=":
/* 450 */               return left.equalsIgnoreCase(right);
/* 451 */             case "!=": return !left.equalsIgnoreCase(right);
/* 452 */             case "<-": return left.toLowerCase().contains(right.toLowerCase());
/* 453 */             case "!<-": return !left.toLowerCase().contains(right.toLowerCase());
/* 454 */             case "|-": return left.toLowerCase().startsWith(right.toLowerCase());
/* 455 */             case "!|-": return !left.toLowerCase().startsWith(right.toLowerCase());
/* 456 */             case "-|": return left.toLowerCase().endsWith(right.toLowerCase());
/* 457 */             case "!-|": return !left.toLowerCase().endsWith(right.toLowerCase()); }
/* 458 */            return false;
/*     */         } 
/*     */       } 
/*     */     } 
/*     */     
/* 463 */     return false;
/*     */   }
/*     */ }


/* Location:              C:\Users\LQDFIRE\Desktop\QQAssistant-1.0.0.jar!\com\allfire\qqassist\handler\ResponseHandler.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */