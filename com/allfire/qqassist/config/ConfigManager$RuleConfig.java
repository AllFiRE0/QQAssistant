/*     */ package com.allfire.qqassist.config;
/*     */ 
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
/*     */ import java.util.regex.Pattern;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class RuleConfig
/*     */ {
/*     */   public String name;
/*     */   public int delayTicks;
/*     */   public String permission;
/*     */   public List<String> allowedChats;
/*     */   public String condition;
/*     */   public int priority;
/*     */   public int chance;
/*     */   public int cooldownTicks;
/*     */   public List<String> questionsExact;
/*     */   public List<String> questionsContains;
/*     */   public List<Pattern> questionsRegex;
/*     */   public List<String> answers;
/*     */   public List<String> randomAnswers;
/*     */   public List<ArgDef> argsDef;
/*     */   public boolean sessionEnabled = false;
/* 182 */   public int sessionTimeout = 600;
/* 183 */   public int sessionIdleTimeout = 300;
/* 184 */   public String sessionIdleMessage = "";
/* 185 */   public String sessionCancelMessage = "";
/* 186 */   public List<String> sessionCancelTriggers = new ArrayList<>();
/* 187 */   public List<String> sessionSkipTriggers = new ArrayList<>();
/* 188 */   public String sessionListenChat = "same";
/* 189 */   public int sessionArgsTimeout = 1200;
/* 190 */   public List<SessionStep> sessionSteps = new ArrayList<>();
/*     */   
/*     */   public RuleConfig() {
/* 193 */     this.allowedChats = new ArrayList<>();
/* 194 */     this.questionsExact = new ArrayList<>();
/* 195 */     this.questionsContains = new ArrayList<>();
/* 196 */     this.questionsRegex = new ArrayList<>();
/* 197 */     this.answers = new ArrayList<>();
/* 198 */     this.randomAnswers = new ArrayList<>();
/* 199 */     this.argsDef = new ArrayList<>();
/* 200 */     this.sessionCancelTriggers = new ArrayList<>();
/* 201 */     this.sessionSkipTriggers = new ArrayList<>();
/* 202 */     this.sessionSteps = new ArrayList<>();
/*     */   }
/*     */   
/*     */   public static class ArgDef {
/*     */     public String id;
/* 207 */     public List<String> values = new ArrayList<>();
/* 208 */     public String defaultValue = "";
/*     */   }
/*     */   
/*     */   public static class SessionStep {
/*     */     public String id;
/*     */     public String prompt;
/*     */     public String validate;
/*     */     public String errorMessage;
/*     */     public String defaultValue;
/*     */   }
/*     */ }


/* Location:              C:\Users\LQDFIRE\Desktop\QQAssistant-1.0.0.jar!\com\allfire\qqassist\config\ConfigManager$RuleConfig.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */