/*    */ package com.allfire.qqassist.chat;
/*    */ 
/*    */ import java.util.List;
/*    */ import org.bukkit.entity.Player;
/*    */ 
/*    */ public class ChatContext {
/*    */   private final String chatId;
/*    */   private final String chatDisplayName;
/*    */   private final List<Player> recipients;
/*    */   private final long timestamp;
/*    */   
/*    */   public ChatContext(String chatId, String chatDisplayName, List<Player> recipients) {
/* 13 */     this.chatId = chatId;
/* 14 */     this.chatDisplayName = chatDisplayName;
/* 15 */     this.recipients = recipients;
/* 16 */     this.timestamp = System.currentTimeMillis();
/*    */   }
/*    */   
/*    */   public String getChatId() {
/* 20 */     return this.chatId;
/*    */   }
/*    */   
/*    */   public String getChatDisplayName() {
/* 24 */     return this.chatDisplayName;
/*    */   }
/*    */   
/*    */   public List<Player> getRecipients() {
/* 28 */     return this.recipients;
/*    */   }
/*    */   
/*    */   public long getTimestamp() {
/* 32 */     return this.timestamp;
/*    */   }
/*    */   
/*    */   public boolean isExpired(long timeoutMillis) {
/* 36 */     return (System.currentTimeMillis() - this.timestamp > timeoutMillis);
/*    */   }
/*    */ }


/* Location:              C:\Users\LQDFIRE\Desktop\QQAssistant-1.0.0.jar!\com\allfire\qqassist\chat\ChatContext.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */