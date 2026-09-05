/*    */ package com.allfire.qqassist;
/*    */ 
/*    */ import com.allfire.qqassist.chat.ChatContextManager;
/*    */ import com.allfire.qqassist.chat.SessionManager;
/*    */ import com.allfire.qqassist.command.QQAssistCommand;
/*    */ import com.allfire.qqassist.config.ConfigManager;
/*    */ import com.allfire.qqassist.expansion.QQAssistExpansion;
/*    */ import com.allfire.qqassist.handler.ResponseHandler;
/*    */ import com.allfire.qqassist.listener.ChatListener;
/*    */ import org.bukkit.command.CommandExecutor;
/*    */ import org.bukkit.event.Listener;
/*    */ import org.bukkit.plugin.Plugin;
/*    */ import org.bukkit.plugin.java.JavaPlugin;
/*    */ 
/*    */ public class QQAssistPlugin
/*    */   extends JavaPlugin
/*    */ {
/*    */   private QQAssistExpansion expansion;
/*    */   private ChatContextManager chatContextManager;
/*    */   
/*    */   public void onEnable() {
/* 22 */     saveDefaultConfig();
/*    */     
/* 24 */     this.configManager = new ConfigManager(this);
/* 25 */     this.chatContextManager = new ChatContextManager();
/* 26 */     this.responseHandler = new ResponseHandler(this);
/* 27 */     this.sessionManager = new SessionManager(this);
/*    */     
/* 29 */     if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
/* 30 */       this.expansion = new QQAssistExpansion(this);
/* 31 */       this.expansion.register();
/*    */       
/* 33 */       getServer().getPluginManager().registerEvents((Listener)new ChatListener(this), (Plugin)this);
/* 34 */       getCommand("qqassistant").setExecutor((CommandExecutor)new QQAssistCommand(this));
/*    */       
/* 36 */       getLogger().info("QQAssist started!");
/*    */     } else {
/* 38 */       getLogger().warning("PlaceholderAPI not found! QQAssist - disabled.");
/* 39 */       getServer().getPluginManager().disablePlugin((Plugin)this);
/*    */     } 
/*    */   }
/*    */   private ConfigManager configManager; private ResponseHandler responseHandler; private SessionManager sessionManager;
/*    */   
/*    */   public void onDisable() {
/* 45 */     if (this.expansion != null && this.expansion.isRegistered()) {
/* 46 */       this.expansion.unregister();
/*    */     }
/* 48 */     if (this.chatContextManager != null) {
/* 49 */       this.chatContextManager.clearAll();
/*    */     }
/* 51 */     if (this.sessionManager != null) {
/* 52 */       this.sessionManager.clearAll();
/*    */     }
/* 54 */     getLogger().info("QQAssist is disabled!");
/*    */   }
/*    */   
/*    */   public QQAssistExpansion getExpansion() {
/* 58 */     return this.expansion;
/*    */   }
/*    */   
/*    */   public ChatContextManager getChatContextManager() {
/* 62 */     return this.chatContextManager;
/*    */   }
/*    */   
/*    */   public ConfigManager getConfigManager() {
/* 66 */     return this.configManager;
/*    */   }
/*    */   
/*    */   public ResponseHandler getResponseHandler() {
/* 70 */     return this.responseHandler;
/*    */   }
/*    */   
/*    */   public SessionManager getSessionManager() {
/* 74 */     return this.sessionManager;
/*    */   }
/*    */ }


/* Location:              C:\Users\LQDFIRE\Desktop\QQAssistant-1.0.0.jar!\com\allfire\qqassist\QQAssistPlugin.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */