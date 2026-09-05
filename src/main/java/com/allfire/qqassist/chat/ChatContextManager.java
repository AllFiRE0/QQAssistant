package com.allfire.qqassist.chat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatContextManager {

    private final Map<UUID, ChatContext> lastChatContexts = new ConcurrentHashMap<>();
    private static final long CONTEXT_TIMEOUT = 60000L;

    public void setLastChatContext(UUID playerId, ChatContext context) {
        this.lastChatContexts.put(playerId, context);
    }

    public ChatContext getLastChatContext(UUID playerId) {
        ChatContext context = this.lastChatContexts.get(playerId);
        if (context != null && context.isExpired(60000L)) {
            this.lastChatContexts.remove(playerId);
            return null;
        }
        return context;
    }

    public void clearContext(UUID playerId) {
        this.lastChatContexts.remove(playerId);
    }

    public void clearAll() {
        this.lastChatContexts.clear();
    }
}