/*    */ package com.allfire.qqassist.chat;
/*    */ 
/*    */ import java.util.Map;
/*    */ import java.util.UUID;
/*    */ import java.util.concurrent.ConcurrentHashMap;
/*    */ 
/*    */ public class ChatContextManager {
/*  8 */   private final Map<UUID, ChatContext> lastChatContexts = new ConcurrentHashMap<>();
/*    */   private static final long CONTEXT_TIMEOUT = 60000L;
/*    */   
/*    */   public void setLastChatContext(UUID playerId, ChatContext context) {
/* 12 */     this.lastChatContexts.put(playerId, context);
/*    */   }
/*    */   
/*    */   public ChatContext getLastChatContext(UUID playerId) {
/* 16 */     ChatContext context = this.lastChatContexts.get(playerId);
/* 17 */     if (context != null && context.isExpired(60000L)) {
/* 18 */       this.lastChatContexts.remove(playerId);
/* 19 */       return null;
/*    */     } 
/* 21 */     return context;
/*    */   }
/*    */   
/*    */   public void clearContext(UUID playerId) {
/* 25 */     this.lastChatContexts.remove(playerId);
/*    */   }
/*    */   
/*    */   public void clearAll() {
/* 29 */     this.lastChatContexts.clear();
/*    */   }
/*    */ }


/* Location:              C:\Users\LQDFIRE\Desktop\QQAssistant-1.0.0.jar!\com\allfire\qqassist\chat\ChatContextManager.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */