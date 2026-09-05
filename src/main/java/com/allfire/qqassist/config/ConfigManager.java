package com.allfire.qqassist.config;

import com.allfire.qqassist.QQAssistPlugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final QQAssistPlugin plugin;
    private FileConfiguration config;
    private List<String> aliases;
    private Map<String, RuleConfig> rules;
    private List<Pattern> mentionPatterns;

    public ConfigManager(QQAssistPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        this.plugin.reloadConfig();
        this.config = this.plugin.getConfig();
        this.aliases = loadAliases();
        this.rules = loadRules();
        this.mentionPatterns = loadMentionPatterns();
    }

    private List<String> loadAliases() {
        List<String> loadedAliases = this.config.getStringList("aliases");
        if (loadedAliases.isEmpty()) {
            loadedAliases = Arrays.asList("qqa", "qqassist", "assist", "assistant", "bot", "chatbot");
        }
        return loadedAliases;
    }

    private List<Pattern> loadMentionPatterns() {
        List<Pattern> patterns = new ArrayList<>();
        List<String> patternStrings = this.config.getStringList("mention-patterns");

        for (String patternString : patternStrings) {
            try {
                String regex = Pattern.quote(patternString).replace("{player}", "\\E([a-zA-Z0-9_]+)\\Q");
                patterns.add(Pattern.compile(regex));
            } catch (PatternSyntaxException e) {
                this.plugin.getLogger().warning("Неверный паттерн упоминания: " + patternString);
            }
        }

        return patterns;
    }

    private Map<String, RuleConfig> loadRules() {
        Map<String, RuleConfig> loadedRules = new LinkedHashMap<>();
        ConfigurationSection rulesSection = this.config.getConfigurationSection("rules");

        if (rulesSection == null) {
            return loadedRules;
        }

        for (String key : rulesSection.getKeys(false)) {
            ConfigurationSection ruleSection = rulesSection.getConfigurationSection(key);
            if (ruleSection == null) continue;
            RuleConfig ruleConfig = new RuleConfig();
            ruleConfig.name = key;
            ruleConfig.delayTicks = ruleSection.getInt("delay_ticks", 0);
            ruleConfig.permission = ruleSection.getString("permission", "");
            ruleConfig.allowedChats = new ArrayList<>(ruleSection.getStringList("allowed-chats"));
            ruleConfig.condition = ruleSection.getString("condition", "");
            ruleConfig.priority = ruleSection.getInt("priority", 100);
            ruleConfig.chance = ruleSection.getInt("chance", 100);
            ruleConfig.cooldownTicks = ruleSection.getInt("cooldown_ticks", 40);

            ConfigurationSection questionsSection = ruleSection.getConfigurationSection("questions");
            if (questionsSection != null) {
                ruleConfig.questionsExact = new ArrayList<>(questionsSection.getStringList("exact"));
                ruleConfig.questionsContains = new ArrayList<>(questionsSection.getStringList("contains"));

                List<String> regexStrings = questionsSection.getStringList("regex");
                ruleConfig.questionsRegex = new ArrayList<>();
                for (String regex : regexStrings) {
                    try {
                        ruleConfig.questionsRegex.add(Pattern.compile(regex, 66));
                    } catch (PatternSyntaxException e) {
                        this.plugin.getLogger().warning("Неверный regex в правиле " + key + ": " + regex);
                    }
                }
            }

            ruleConfig.answers = new ArrayList<>(ruleSection.getStringList("answers"));
            ruleConfig.randomAnswers = new ArrayList<>(ruleSection.getStringList("random_answers"));

            List<Map<?, ?>> argsDefList = ruleSection.getMapList("args-def");
            for (Map<?, ?> defMap : argsDefList) {
                RuleConfig.ArgDef argDef = new RuleConfig.ArgDef();
                argDef.id = (String) defMap.get("id");
                Object valuesObj = defMap.get("values");
                if (valuesObj instanceof List) {
                    for (Object item : (List<?>) valuesObj) {
                        if (item instanceof String) {
                            argDef.values.add((String) item);
                        }
                    }
                }
                Object defaultObj = defMap.get("default");
                argDef.defaultValue = (defaultObj instanceof String) ? (String) defaultObj : "";
                ruleConfig.argsDef.add(argDef);
            }

            ConfigurationSection sessionSection = ruleSection.getConfigurationSection("session");
            if (sessionSection != null) {
                ruleConfig.sessionEnabled = sessionSection.getBoolean("enabled", false);
                ruleConfig.sessionTimeout = sessionSection.getInt("timeout", 600);
                ruleConfig.sessionIdleTimeout = sessionSection.getInt("idle-timeout", 300);
                ruleConfig.sessionIdleMessage = sessionSection.getString("idle-message", "");
                ruleConfig.sessionCancelMessage = sessionSection.getString("cancel-message", "");
                ruleConfig.sessionCancelTriggers = sessionSection.getStringList("cancel-triggers");
                ruleConfig.sessionSkipTriggers = sessionSection.getStringList("skip-triggers");
                ruleConfig.sessionListenChat = sessionSection.getString("listen-chat", "same");
                ruleConfig.sessionArgsTimeout = sessionSection.getInt("args-timeout", 1200);

                List<Map<?, ?>> stepsList = sessionSection.getMapList("steps");
                for (Map<?, ?> stepMap : stepsList) {
                    RuleConfig.SessionStep step = new RuleConfig.SessionStep();
                    step.id = (String) stepMap.get("id");
                    step.prompt = (String) stepMap.get("prompt");
                    step.validate = (String) stepMap.get("validate");
                    step.errorMessage = (String) stepMap.get("error");
                    step.defaultValue = (String) stepMap.get("default");
                    ruleConfig.sessionSteps.add(step);
                }
            }

            loadedRules.put(key, ruleConfig);
        }

        List<Map.Entry<String, RuleConfig>> sortedRules = new ArrayList<>(loadedRules.entrySet());
        sortedRules.sort((a, b) -> Integer.compare(b.getValue().priority, a.getValue().priority));

        Map<String, RuleConfig> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, RuleConfig> entry : sortedRules) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public List<String> getAliases() {
        return this.aliases;
    }

    public Map<String, RuleConfig> getRules() {
        return this.rules;
    }

    public List<Pattern> getMentionPatterns() {
        return this.mentionPatterns;
    }

    public String getPrefix() {
        return this.config.getString("settings.prefix", "&6QQAssist: ");
    }

    public boolean isDebug() {
        return this.config.getBoolean("settings.debug", false);
    }

    public int getSessionTimeout() {
        return this.config.getInt("settings.session-timeout", 60);
    }

    public String getMessage(String key) {
        return this.config.getString("messages." + key, "");
    }

    public static class RuleConfig {
        public String name;
        public int delayTicks;
        public String permission;
        public List<String> allowedChats;
        public String condition;
        public int priority;
        public int chance;
        public int cooldownTicks;
        public List<String> questionsExact;
        public List<String> questionsContains;
        public List<Pattern> questionsRegex;
        public List<String> answers;
        public List<String> randomAnswers;
        public List<ArgDef> argsDef;
        public boolean sessionEnabled = false;
        public int sessionTimeout = 600;
        public int sessionIdleTimeout = 300;
        public String sessionIdleMessage = "";
        public String sessionCancelMessage = "";
        public List<String> sessionCancelTriggers = new ArrayList<>();
        public List<String> sessionSkipTriggers = new ArrayList<>();
        public String sessionListenChat = "same";
        public int sessionArgsTimeout = 1200;
        public List<SessionStep> sessionSteps = new ArrayList<>();

        public RuleConfig() {
            this.allowedChats = new ArrayList<>();
            this.questionsExact = new ArrayList<>();
            this.questionsContains = new ArrayList<>();
            this.questionsRegex = new ArrayList<>();
            this.answers = new ArrayList<>();
            this.randomAnswers = new ArrayList<>();
            this.argsDef = new ArrayList<>();
            this.sessionCancelTriggers = new ArrayList<>();
            this.sessionSkipTriggers = new ArrayList<>();
            this.sessionSteps = new ArrayList<>();
        }

        public static class ArgDef {
            public String id;
            public List<String> values = new ArrayList<>();
            public String defaultValue = "";
        }

        public static class SessionStep {
            public String id;
            public String prompt;
            public String validate;
            public String errorMessage;
            public String defaultValue;
        }
    }
}