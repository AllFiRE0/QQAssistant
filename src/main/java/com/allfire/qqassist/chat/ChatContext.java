package com.allfire.qqassist.chat;

import java.util.List;
import org.bukkit.entity.Player;

public class ChatContext {

    private final String chatId;
    private final String chatDisplayName;
    private final List<Player> recipients;
    private final long timestamp;

    public ChatContext(String chatId, String chatDisplayName, List<Player> recipients) {
        this.chatId = chatId;
        this.chatDisplayName = chatDisplayName;
        this.recipients = recipients;
        this.timestamp = System.currentTimeMillis();
    }

    public String getChatId() {
        return this.chatId;
    }

    public String getChatDisplayName() {
        return this.chatDisplayName;
    }

    public List<Player> getRecipients() {
        return this.recipients;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public boolean isExpired(long timeoutMillis) {
        return System.currentTimeMillis() - this.timestamp > timeoutMillis;
    }
}