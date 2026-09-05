/*     */ package com.allfire.qqassist.config;
/*     */ import com.allfire.qqassist.QQAssistPlugin;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Arrays;
/*     */ import java.util.LinkedHashMap;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.regex.Pattern;
/*     */ import java.util.regex.PatternSyntaxException;
/*     */ import org.bukkit.configuration.ConfigurationSection;
/*     */ 
/*     */ public class ConfigManager {
/*     */   private final QQAssistPlugin plugin;
/*     */   private FileConfiguration config;
/*     */   private List<String> aliases;
/*     */   private Map<String, RuleConfig> rules;
/*     */   private List<Pattern> mentionPatterns;
/*     */   
/*     */   public ConfigManager(QQAssistPlugin plugin) {
/*  20 */     this.plugin = plugin;
/*  21 */     loadConfig();
/*     */   }
/*     */   
/*     */   public void loadConfig() {
/*  25 */     this.plugin.reloadConfig();
/*  26 */     this.config = this.plugin.getConfig();
/*  27 */     this.aliases = loadAliases();
/*  28 */     this.rules = loadRules();
/*  29 */     this.mentionPatterns = loadMentionPatterns();
/*     */   }
/*     */   
/*     */   private List<String> loadAliases() {
/*  33 */     List<String> loadedAliases = this.config.getStringList("aliases");
/*  34 */     if (loadedAliases.isEmpty()) {
/*  35 */       loadedAliases = Arrays.asList(new String[] { "qqa", "qqassist", "assist", "assistant", "bot", "chatbot" });
/*     */     }
/*  37 */     return loadedAliases;
/*     */   }
/*     */   
/*     */   private List<Pattern> loadMentionPatterns() {
/*  41 */     List<Pattern> patterns = new ArrayList<>();
/*  42 */     List<String> patternStrings = this.config.getStringList("mention-patterns");
/*     */     
/*  44 */     for (String patternString : patternStrings) {
/*     */       
/*     */       try {
/*  47 */         String regex = Pattern.quote(patternString).replace("{player}", "\\E([a-zA-Z0-9_]+)\\Q");
/*  48 */         patterns.add(Pattern.compile(regex));
/*  49 */       } catch (PatternSyntaxException e) {
/*  50 */         this.plugin.getLogger().warning("Неверный паттерн упоминания: " + patternString);
/*     */       } 
/*     */     } 
/*     */     
/*  54 */     return patterns;
/*     */   }
/*     */   
/*     */   private Map<String, RuleConfig> loadRules() {
/*  58 */     Map<String, RuleConfig> loadedRules = new LinkedHashMap<>();
/*  59 */     ConfigurationSection rulesSection = this.config.getConfigurationSection("rules");
/*     */     
/*  61 */     if (rulesSection == null) {
/*  62 */       return loadedRules;
/*     */     }
/*     */     
/*  65 */     for (String key : rulesSection.getKeys(false)) {
/*  66 */       ConfigurationSection ruleSection = rulesSection.getConfigurationSection(key);
/*  67 */       if (ruleSection == null)
/*     */         continue; 
/*  69 */       RuleConfig ruleConfig = new RuleConfig();
/*  70 */       ruleConfig.name = key;
/*  71 */       ruleConfig.delayTicks = ruleSection.getInt("delay_ticks", 0);
/*  72 */       ruleConfig.permission = ruleSection.getString("permission", "");
/*  73 */       ruleConfig.allowedChats = new ArrayList<>(ruleSection.getStringList("allowed-chats"));
/*  74 */       ruleConfig.condition = ruleSection.getString("condition", "");
/*  75 */       ruleConfig.priority = ruleSection.getInt("priority", 100);
/*  76 */       ruleConfig.chance = ruleSection.getInt("chance", 100);
/*  77 */       ruleConfig.cooldownTicks = ruleSection.getInt("cooldown_ticks", 40);
/*     */ 
/*     */       
/*  80 */       ConfigurationSection questionsSection = ruleSection.getConfigurationSection("questions");
/*  81 */       if (questionsSection != null) {
/*  82 */         ruleConfig.questionsExact = new ArrayList<>(questionsSection.getStringList("exact"));
/*  83 */         ruleConfig.questionsContains = new ArrayList<>(questionsSection.getStringList("contains"));
/*     */         
/*  85 */         List<String> regexStrings = questionsSection.getStringList("regex");
/*  86 */         ruleConfig.questionsRegex = new ArrayList<>();
/*  87 */         for (String regex : regexStrings) {
/*     */           try {
/*  89 */             ruleConfig.questionsRegex.add(Pattern.compile(regex, 66));
/*  90 */           } catch (PatternSyntaxException e) {
/*  91 */             this.plugin.getLogger().warning("Неверный regex в правиле " + key + ": " + regex);
/*     */           } 
/*     */         } 
/*     */       } 
/*     */ 
/*     */       
/*  97 */       ruleConfig.answers = new ArrayList<>(ruleSection.getStringList("answers"));
/*  98 */       ruleConfig.randomAnswers = new ArrayList<>(ruleSection.getStringList("random_answers"));
/*     */ 
/*     */       
/* 101 */       List<Map<?, ?>> argsDefList = ruleSection.getMapList("args-def");
/* 102 */       for (Map<?, ?> defMap : argsDefList) {
/* 103 */         RuleConfig.ArgDef argDef = new RuleConfig.ArgDef();
/* 104 */         argDef.id = (String)defMap.get("id");
/* 105 */         Object valuesObj = defMap.get("values");
/* 106 */         if (valuesObj instanceof List) {
/* 107 */           for (Object item : valuesObj) {
/* 108 */             if (item instanceof String) {
/* 109 */               argDef.values.add((String)item);
/*     */             }
/*     */           } 
/*     */         }
/* 113 */         Object defaultObj = defMap.get("default");
/* 114 */         argDef.defaultValue = (defaultObj instanceof String) ? (String)defaultObj : "";
/* 115 */         ruleConfig.argsDef.add(argDef);
/*     */       } 
/*     */ 
/*     */       
/* 119 */       ConfigurationSection sessionSection = ruleSection.getConfigurationSection("session");
/* 120 */       if (sessionSection != null) {
/* 121 */         ruleConfig.sessionEnabled = sessionSection.getBoolean("enabled", false);
/* 122 */         ruleConfig.sessionTimeout = sessionSection.getInt("timeout", 600);
/* 123 */         ruleConfig.sessionIdleTimeout = sessionSection.getInt("idle-timeout", 300);
/* 124 */         ruleConfig.sessionIdleMessage = sessionSection.getString("idle-message", "");
/* 125 */         ruleConfig.sessionCancelMessage = sessionSection.getString("cancel-message", "");
/* 126 */         ruleConfig.sessionCancelTriggers = sessionSection.getStringList("cancel-triggers");
/* 127 */         ruleConfig.sessionSkipTriggers = sessionSection.getStringList("skip-triggers");
/* 128 */         ruleConfig.sessionListenChat = sessionSection.getString("listen-chat", "same");
/* 129 */         ruleConfig.sessionArgsTimeout = sessionSection.getInt("args-timeout", 1200);
/*     */         
/* 131 */         List<Map<?, ?>> stepsList = sessionSection.getMapList("steps");
/* 132 */         for (Map<?, ?> stepMap : stepsList) {
/* 133 */           RuleConfig.SessionStep step = new RuleConfig.SessionStep();
/* 134 */           step.id = (String)stepMap.get("id");
/* 135 */           step.prompt = (String)stepMap.get("prompt");
/* 136 */           step.validate = (String)stepMap.get("validate");
/* 137 */           step.errorMessage = (String)stepMap.get("error");
/* 138 */           step.defaultValue = (String)stepMap.get("default");
/* 139 */           ruleConfig.sessionSteps.add(step);
/*     */         } 
/*     */       } 
/*     */       
/* 143 */       loadedRules.put(key, ruleConfig);
/*     */     } 
/*     */ 
/*     */     
/* 147 */     List<Map.Entry<String, RuleConfig>> sortedRules = new ArrayList<>(loadedRules.entrySet());
/* 148 */     sortedRules.sort((a, b) -> Integer.compare(((RuleConfig)b.getValue()).priority, ((RuleConfig)a.getValue()).priority));
/*     */     
/* 150 */     Map<String, RuleConfig> sortedMap = new LinkedHashMap<>();
/* 151 */     for (Map.Entry<String, RuleConfig> entry : sortedRules) {
/* 152 */       sortedMap.put(entry.getKey(), entry.getValue());
/*     */     }
/*     */     
/* 155 */     return sortedMap;
/*     */   }
/*     */   
/* 158 */   public List<String> getAliases() { return this.aliases; }
/* 159 */   public Map<String, RuleConfig> getRules() { return this.rules; }
/* 160 */   public List<Pattern> getMentionPatterns() { return this.mentionPatterns; }
/* 161 */   public String getPrefix() { return this.config.getString("settings.prefix", "&6QQAssist: "); }
/* 162 */   public boolean isDebug() { return this.config.getBoolean("settings.debug", false); }
/* 163 */   public int getSessionTimeout() { return this.config.getInt("settings.session-timeout", 60); } public String getMessage(String key) {
/* 164 */     return this.config.getString("messages." + key, "");
/*     */   }
/*     */   
/*     */   public static class RuleConfig { public String name;
/*     */     public int delayTicks;
/*     */     public String permission;
/*     */     public List<String> allowedChats;
/*     */     public String condition;
/*     */     public int priority;
/*     */     public int chance;
/*     */     public int cooldownTicks;
/*     */     public List<String> questionsExact;
/*     */     public List<String> questionsContains;
/*     */     public List<Pattern> questionsRegex;
/*     */     public List<String> answers;
/*     */     public List<String> randomAnswers;
/*     */     public List<ArgDef> argsDef;
/*     */     public boolean sessionEnabled = false;
/* 182 */     public int sessionTimeout = 600;
/* 183 */     public int sessionIdleTimeout = 300;
/* 184 */     public String sessionIdleMessage = "";
/* 185 */     public String sessionCancelMessage = "";
/* 186 */     public List<String> sessionCancelTriggers = new ArrayList<>();
/* 187 */     public List<String> sessionSkipTriggers = new ArrayList<>();
/* 188 */     public String sessionListenChat = "same";
/* 189 */     public int sessionArgsTimeout = 1200;
/* 190 */     public List<SessionStep> sessionSteps = new ArrayList<>();
/*     */     
/*     */     public RuleConfig() {
/* 193 */       this.allowedChats = new ArrayList<>();
/* 194 */       this.questionsExact = new ArrayList<>();
/* 195 */       this.questionsContains = new ArrayList<>();
/* 196 */       this.questionsRegex = new ArrayList<>();
/* 197 */       this.answers = new ArrayList<>();
/* 198 */       this.randomAnswers = new ArrayList<>();
/* 199 */       this.argsDef = new ArrayList<>();
/* 200 */       this.sessionCancelTriggers = new ArrayList<>();
/* 201 */       this.sessionSkipTriggers = new ArrayList<>();
/* 202 */       this.sessionSteps = new ArrayList<>();
/*     */     }
/*     */     
/*     */     public static class ArgDef {
/*     */       public String id;
/* 207 */       public List<String> values = new ArrayList<>();
/* 208 */       public String defaultValue = ""; } public static class SessionStep { public String id; public String prompt; public String validate; public String errorMessage; public String defaultValue; } } public static class ArgDef { public String defaultValue = "";
/*     */     public String id;
/*     */     public List<String> values = new ArrayList<>(); }
/*     */ 
/*     */   
/*     */   public static class SessionStep {
/*     */     public String id;
/*     */     public String prompt;
/*     */     public String validate;
/*     */     public String errorMessage;
/*     */     public String defaultValue;
/*     */   }
/*     */ }


/* Location:              C:\Users\LQDFIRE\Desktop\QQAssistant-1.0.0.jar!\com\allfire\qqassist\config\ConfigManager.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */