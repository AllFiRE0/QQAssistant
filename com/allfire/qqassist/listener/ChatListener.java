/*     */ package com.allfire.qqassist.listener;
/*     */ 
/*     */ import com.allfire.qqassist.QQAssistPlugin;
/*     */ import java.util.List;
/*     */ import java.util.regex.Matcher;
/*     */ import java.util.regex.Pattern;
/*     */ import org.bukkit.Bukkit;
/*     */ import org.bukkit.OfflinePlayer;
/*     */ import org.bukkit.entity.Player;
/*     */ import org.bukkit.event.EventHandler;
/*     */ import org.bukkit.event.EventPriority;
/*     */ import org.bukkit.event.Listener;
/*     */ import org.bukkit.event.player.AsyncPlayerChatEvent;
/*     */ import ru.brikster.chatty.api.event.ChattyMessageEvent;
/*     */ 
/*     */ 
/*     */ 
/*     */ public class ChatListener
/*     */   implements Listener
/*     */ {
/*     */   private final QQAssistPlugin plugin;
/*     */   private boolean chattyEnabled;
/*     */   
/*     */   public ChatListener(QQAssistPlugin plugin) {
/*  25 */     this.plugin = plugin;
/*  26 */     this.chattyEnabled = Bukkit.getPluginManager().isPluginEnabled("Chatty");
/*  27 */     log("Chatty: " + (this.chattyEnabled ? "да" : "нет"));
/*     */   }
/*     */   
/*     */   @EventHandler(priority = EventPriority.MONITOR)
/*     */   public void onChattyMessage(ChattyMessageEvent event) {
/*  32 */     if (!this.chattyEnabled)
/*     */       return; 
/*  34 */     Player player = event.getSender();
/*  35 */     String message = event.getPlainMessage();
/*  36 */     this.plugin.getExpansion().setLastMessage(player.getUniqueId(), message);
/*  37 */     String chatId = event.getChat().getId();
/*  38 */     List<Player> recipients = event.getRecipients();
/*     */     
/*  40 */     log("[Chatty] chatId=" + chatId + " msg=" + message);
/*     */ 
/*     */     
/*  43 */     if (this.plugin.getSessionManager().isInSession(player.getUniqueId())) {
/*  44 */       boolean handled = this.plugin.getSessionManager().handleSessionMessage(player, message);
/*  45 */       if (handled)
/*     */         return; 
/*     */     } 
/*  48 */     String mentionedPlayer = findMentionedPlayer(message, player.getName());
/*  49 */     log("[Chatty] foundMention: " + mentionedPlayer);
/*     */     
/*  51 */     if (mentionedPlayer != null) {
/*  52 */       this.plugin.getExpansion().setTargetPlayer(player.getUniqueId(), mentionedPlayer);
/*     */     } else {
/*  54 */       this.plugin.getExpansion().clearTargetPlayer(player.getUniqueId());
/*     */     } 
/*     */     
/*  57 */     this.plugin.getResponseHandler().processMessage(player, message, chatId);
/*     */   }
/*     */   
/*     */   @EventHandler(priority = EventPriority.MONITOR)
/*     */   public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
/*  62 */     if (event.isCancelled())
/*  63 */       return;  if (this.chattyEnabled)
/*     */       return; 
/*  65 */     Player player = event.getPlayer();
/*  66 */     String message = event.getMessage();
/*  67 */     this.plugin.getExpansion().setLastMessage(player.getUniqueId(), message);
/*     */     
/*  69 */     log("[Async] msg=" + message);
/*     */ 
/*     */     
/*  72 */     if (this.plugin.getSessionManager().isInSession(player.getUniqueId())) {
/*  73 */       boolean handled = this.plugin.getSessionManager().handleSessionMessage(player, message);
/*  74 */       if (handled)
/*     */         return; 
/*     */     } 
/*  77 */     String mentionedPlayer = findMentionedPlayer(message, player.getName());
/*  78 */     log("[Async] foundMention: " + mentionedPlayer);
/*     */     
/*  80 */     if (mentionedPlayer != null) {
/*  81 */       this.plugin.getExpansion().setTargetPlayer(player.getUniqueId(), mentionedPlayer);
/*     */     } else {
/*  83 */       this.plugin.getExpansion().clearTargetPlayer(player.getUniqueId());
/*     */     } 
/*     */     
/*  86 */     this.plugin.getResponseHandler().processMessage(player, message, "default");
/*     */   }
/*     */   
/*     */   private String findMentionedPlayer(String message, String senderName) {
/*  90 */     List<Pattern> patterns = this.plugin.getConfigManager().getMentionPatterns();
/*  91 */     log("[findMention] patterns count: " + patterns.size() + " msg: " + message);
/*     */     
/*  93 */     for (Pattern pattern : patterns) {
/*  94 */       log("[findMention] trying pattern: " + pattern.pattern());
/*  95 */       Matcher matcher = pattern.matcher(message);
/*  96 */       if (matcher.find()) {
/*  97 */         String name = matcher.group(1);
/*  98 */         log("[findMention] raw name: '" + name + "'");
/*  99 */         name = name.replaceAll("[?!.,:;)\\]]+$", "");
/* 100 */         log("[findMention] cleaned name: '" + name + "'");
/* 101 */         if (name.length() > 0 && !name.equalsIgnoreCase(senderName)) {
/*     */           
/* 103 */           Player onlineTarget = Bukkit.getPlayer(name);
/* 104 */           if (onlineTarget != null && onlineTarget.isOnline()) {
/* 105 */             log("[findMention] target player: " + name + " (online)");
/* 106 */             return name;
/*     */           } 
/*     */           
/* 109 */           OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
/* 110 */           if (offline != null && offline.hasPlayedBefore()) {
/* 111 */             log("[findMention] target player: " + name + " (offline)");
/* 112 */             return name;
/*     */           } 
/* 114 */           log("[findMention] target player: null");
/*     */         } 
/*     */       } 
/*     */     } 
/* 118 */     return null;
/*     */   }
/*     */   
/*     */   private void log(String message) {
/* 122 */     if (this.plugin.getConfig().getBoolean("settings.debug", false))
/* 123 */       this.plugin.getLogger().info(message); 
/*     */   }
/*     */ }


/* Location:              C:\Users\LQDFIRE\Desktop\QQAssistant-1.0.0.jar!\com\allfire\qqassist\listener\ChatListener.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */