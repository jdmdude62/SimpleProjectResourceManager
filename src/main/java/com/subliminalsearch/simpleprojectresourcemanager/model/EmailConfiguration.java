package com.subliminalsearch.simpleprojectresourcemanager.model;

import java.util.prefs.Preferences;

public class EmailConfiguration {
    private String smtpServer;
    private int smtpPort;
    private String username;
    private String password;
    private boolean useSSL;
    private boolean useTLS;
    private int connectionTimeout;
    private String fromAddress;
    private String fromName;
    private boolean enabled;
    
    // For Exchange Server authentication
    private boolean useNTLMAuth;
    private String domain;
    
    private static final String PREF_SMTP_SERVER = "email.smtp.server";
    private static final String PREF_SMTP_PORT = "email.smtp.port";
    private static final String PREF_USERNAME = "email.username";
    private static final String PREF_PASSWORD = "email.password";
    private static final String PREF_USE_SSL = "email.use.ssl";
    private static final String PREF_USE_TLS = "email.use.tls";
    private static final String PREF_TIMEOUT = "email.timeout";
    private static final String PREF_FROM_ADDRESS = "email.from.address";
    private static final String PREF_FROM_NAME = "email.from.name";
    private static final String PREF_ENABLED = "email.enabled";
    private static final String PREF_USE_NTLM = "email.use.ntlm";
    private static final String PREF_DOMAIN = "email.domain";
    
    private static final Preferences prefs = Preferences.userNodeForPackage(EmailConfiguration.class);
    
    public EmailConfiguration() {
        // Default values for Exchange Server
        this.smtpServer = "";
        this.smtpPort = 587; // Default for TLS
        this.username = "";
        this.password = "";
        this.useSSL = false;
        this.useTLS = true;
        this.connectionTimeout = 30000; // 30 seconds
        this.fromAddress = "";
        this.fromName = "Project Resource Manager";
        this.enabled = false;
        this.useNTLMAuth = false;
        this.domain = "";
    }
    
    public static EmailConfiguration load() {
        EmailConfiguration config = new EmailConfiguration();
        config.setSmtpServer(prefs.get(PREF_SMTP_SERVER, ""));
        config.setSmtpPort(prefs.getInt(PREF_SMTP_PORT, 587));
        config.setUsername(prefs.get(PREF_USERNAME, ""));
        config.setPassword(prefs.get(PREF_PASSWORD, ""));
        config.setUseSSL(prefs.getBoolean(PREF_USE_SSL, false));
        config.setUseTLS(prefs.getBoolean(PREF_USE_TLS, true));
        config.setConnectionTimeout(prefs.getInt(PREF_TIMEOUT, 30000));
        config.setFromAddress(prefs.get(PREF_FROM_ADDRESS, ""));
        config.setFromName(prefs.get(PREF_FROM_NAME, "Project Resource Manager"));
        config.setEnabled(prefs.getBoolean(PREF_ENABLED, false));
        config.setUseNTLMAuth(prefs.getBoolean(PREF_USE_NTLM, false));
        config.setDomain(prefs.get(PREF_DOMAIN, ""));
        return config;
    }
    
    public void save() {
        prefs.put(PREF_SMTP_SERVER, smtpServer != null ? smtpServer : "");
        prefs.putInt(PREF_SMTP_PORT, smtpPort);
        prefs.put(PREF_USERNAME, username != null ? username : "");
        prefs.put(PREF_PASSWORD, password != null ? password : "");
        prefs.putBoolean(PREF_USE_SSL, useSSL);
        prefs.putBoolean(PREF_USE_TLS, useTLS);
        prefs.putInt(PREF_TIMEOUT, connectionTimeout);
        prefs.put(PREF_FROM_ADDRESS, fromAddress != null ? fromAddress : "");
        prefs.put(PREF_FROM_NAME, fromName != null ? fromName : "");
        prefs.putBoolean(PREF_ENABLED, enabled);
        prefs.putBoolean(PREF_USE_NTLM, useNTLMAuth);
        prefs.put(PREF_DOMAIN, domain != null ? domain : "");
    }
    
    public boolean isConfigured() {
        return enabled && 
               smtpServer != null && !smtpServer.trim().isEmpty() &&
               smtpPort > 0 &&
               username != null && !username.trim().isEmpty() &&
               fromAddress != null && !fromAddress.trim().isEmpty();
    }
    
    // Getters and setters
    public String getSmtpServer() {
        return smtpServer;
    }
    
    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }
    
    public int getSmtpPort() {
        return smtpPort;
    }
    
    public void setSmtpPort(int smtpPort) {
        this.smtpPort = smtpPort;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public boolean isUseSSL() {
        return useSSL;
    }
    
    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }
    
    public boolean isUseTLS() {
        return useTLS;
    }
    
    public void setUseTLS(boolean useTLS) {
        this.useTLS = useTLS;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public String getFromAddress() {
        return fromAddress;
    }
    
    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }
    
    public String getFromName() {
        return fromName;
    }
    
    public void setFromName(String fromName) {
        this.fromName = fromName;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isUseNTLMAuth() {
        return useNTLMAuth;
    }
    
    public void setUseNTLMAuth(boolean useNTLMAuth) {
        this.useNTLMAuth = useNTLMAuth;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
}