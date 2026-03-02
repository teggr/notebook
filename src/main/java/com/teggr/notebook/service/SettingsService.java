package com.teggr.notebook.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

@Service
public class SettingsService {

    private final Path configFile = Path.of(System.getProperty("user.home"), ".notebook", "config.properties");

    public SettingsService() {
        try {
            Files.createDirectories(configFile.getParent());
        } catch (IOException e) {
            // ignore
        }
    }

    public Properties load() {
        Properties props = new Properties();
        if (Files.exists(configFile)) {
            try (InputStream in = Files.newInputStream(configFile)) {
                props.load(in);
            } catch (IOException e) {
                // ignore
            }
        }
        return props;
    }

    public void save(String remoteUrl, String token) throws IOException {
        Properties props = load();
        if (remoteUrl != null) props.setProperty("remote.url", remoteUrl);
        if (token != null) props.setProperty("github.token", token);
        try (OutputStream out = Files.newOutputStream(configFile)) {
            props.store(out, "Notebook configuration");
        }
    }

    public String getRemoteUrl() {
        return load().getProperty("remote.url", "");
    }

    public String getToken() {
        return load().getProperty("github.token", "");
    }
}
