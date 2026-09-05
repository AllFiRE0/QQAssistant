package com.allfire.qqassist;

import com.allfire.qqassist.chat.ChatContextManager;
import com.allfire.qqassist.chat.SessionManager;
import com.allfire.qqassist.command.QQAssistCommand;
import com.allfire.qqassist.config.ConfigManager;
import com.allfire.qqassist.expansion.QQAssistExpansion;
import com.allfire.qqassist.handler.ResponseHandler;
import com.allfire.qqassist.listener.ChatListener;
import org.bukkit.plugin.java.JavaPlugin;

public class QQAssistPlugin extends JavaPlugin {

    private QQAssistExpansion expansion;
    private ChatContextManager chatContextManager;
    private ConfigManager configManager;
    private ResponseHandler responseHandler;
    private SessionManager sessionManager;

    public void onEnable() {
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.chatContextManager = new ChatContextManager();
        this.responseHandler = new ResponseHandler(this);
        this.sessionManager = new SessionManager(this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.expansion = new QQAssistExpansion(this);
            this.expansion.register();

            getServer().getPluginManager().registerEvents(new ChatListener(this), this);
            getCommand("qqassistant").setExecutor(new QQAssistCommand(this));

            getLogger().info("QQAssist started!");
        } else {
            getLogger().warning("PlaceholderAPI not found! QQAssist - disabled.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    public void onDisable() {
        if (this.expansion != null && this.expansion.isRegistered()) {
            this.expansion.unregister();
        }
        if (this.chatContextManager != null) {
            this.chatContextManager.clearAll();
        }
        if (this.sessionManager != null) {
            this.sessionManager.clearAll();
        }
        getLogger().info("QQAssist is disabled!");
    }

    public QQAssistExpansion getExpansion() {
        return this.expansion;
    }

    public ChatContextManager getChatContextManager() {
        return this.chatContextManager;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public ResponseHandler getResponseHandler() {
        return this.responseHandler;
    }

    public SessionManager getSessionManager() {
        return this.sessionManager;
    }
}