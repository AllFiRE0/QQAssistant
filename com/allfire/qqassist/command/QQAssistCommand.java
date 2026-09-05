/*    */ package com.allfire.qqassist.command;
/*    */ 
/*    */ import com.allfire.qqassist.QQAssistPlugin;
/*    */ import java.util.List;
/*    */ import net.kyori.adventure.text.Component;
/*    */ import net.kyori.adventure.text.TextComponent;
/*    */ import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
/*    */ import org.bukkit.command.Command;
/*    */ import org.bukkit.command.CommandExecutor;
/*    */ import org.bukkit.command.CommandSender;
/*    */ 
/*    */ public class QQAssistCommand
/*    */   implements CommandExecutor
/*    */ {
/*    */   private final QQAssistPlugin plugin;
/*    */   
/*    */   public QQAssistCommand(QQAssistPlugin plugin) {
/* 18 */     this.plugin = plugin;
/*    */   }
/*    */ 
/*    */   
/*    */   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
/* 23 */     if (!sender.hasPermission("qqassist.admin")) {
/* 24 */       sendMessage(sender, this.plugin.getConfigManager().getMessage("no_permission"));
/* 25 */       return true;
/*    */     } 
/*    */     
/* 28 */     if (args.length == 0) {
/*    */       
/* 30 */       List<String> infoMessages = this.plugin.getConfig().getStringList("messages.info_message");
/* 31 */       for (String message : infoMessages) {
/* 32 */         sendMessage(sender, message.replace("%alias%", label));
/*    */       }
/* 34 */       return true;
/*    */     } 
/*    */     
/* 37 */     switch (args[0].toLowerCase())
/*    */     { case "reload":
/* 39 */         handleReload(sender, label);
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */         
/* 49 */         return true;case "info": handleInfo(sender, label); return true; }  sendMessage(sender, this.plugin.getConfigManager().getMessage("unknown_command")); return true;
/*    */   }
/*    */   
/*    */   private void handleReload(CommandSender sender, String label) {
/* 53 */     long startTime = System.currentTimeMillis();
/*    */     
/* 55 */     this.plugin.getConfigManager().loadConfig();
/*    */     
/* 57 */     long timeTaken = System.currentTimeMillis() - startTime;
/*    */ 
/*    */     
/* 60 */     String message = this.plugin.getConfigManager().getMessage("reload_success").replace("%time%", String.valueOf(timeTaken)).replace("%alias%", label);
/*    */     
/* 62 */     sendMessage(sender, message);
/*    */     
/* 64 */     if (this.plugin.getConfigManager().isDebug()) {
/* 65 */       this.plugin.getLogger().info("Конфигурация перезагружена за " + timeTaken + "ms");
/*    */     }
/*    */   }
/*    */   
/*    */   private void handleInfo(CommandSender sender, String label) {
/* 70 */     List<String> infoMessages = this.plugin.getConfig().getStringList("messages.info_message");
/* 71 */     for (String message : infoMessages) {
/* 72 */       sendMessage(sender, message.replace("%alias%", label));
/*    */     }
/*    */   }
/*    */   
/*    */   private void sendMessage(CommandSender sender, String message) {
/* 77 */     if (message == null || message.isEmpty())
/*    */       return; 
/* 79 */     String prefix = this.plugin.getConfigManager().getPrefix();
/* 80 */     message = message.replace("%prefix%", prefix);
/*    */     
/* 82 */     TextComponent textComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
/* 83 */     sender.sendMessage((Component)textComponent);
/*    */   }
/*    */ }


/* Location:              C:\Users\LQDFIRE\Desktop\QQAssistant-1.0.0.jar!\com\allfire\qqassist\command\QQAssistCommand.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */