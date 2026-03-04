package com.teggr.notebook.controller;

import com.teggr.notebook.model.Settings;
import com.teggr.notebook.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/settings")
public class SettingsApiController {

    private final SettingsService settingsService;

    public SettingsApiController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public Settings getSettings() {
        return new Settings(settingsService.getRemoteUrl(), settingsService.getToken());
    }

    @PutMapping
    public ResponseEntity<Settings> saveSettings(@RequestBody Settings settings) throws IOException {
        settingsService.save(settings.getRemoteUrl(), settings.getToken());
        return ResponseEntity.ok(new Settings(settingsService.getRemoteUrl(), settingsService.getToken()));
    }
}
