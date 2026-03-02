package com.teggr.notebook.controller;

import com.teggr.notebook.service.SettingsService;
import com.teggr.notebook.template.HtmlTemplates;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Controller
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping(value = "/settings", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String settingsPage() {
        return HtmlTemplates.settingsPage(settingsService.getRemoteUrl(), settingsService.getToken(), null);
    }

    @PostMapping(value = "/settings")
    @ResponseBody
    public String saveSettings(@RequestParam(value="remoteUrl", defaultValue="") String remoteUrl,
                               @RequestParam(value="token", defaultValue="") String token) throws IOException {
        settingsService.save(remoteUrl, token);
        return HtmlTemplates.settingsPage(remoteUrl, token, "Settings saved!");
    }
}
