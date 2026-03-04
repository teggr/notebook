package com.teggr.notebook.model;

public class Settings {
    private String remoteUrl;
    private String token;

    public Settings() {}

    public Settings(String remoteUrl, String token) {
        this.remoteUrl = remoteUrl;
        this.token = token;
    }

    public String getRemoteUrl() { return remoteUrl; }
    public void setRemoteUrl(String remoteUrl) { this.remoteUrl = remoteUrl; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
