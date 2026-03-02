package com.teggr.notebook.controller;

import com.teggr.notebook.model.SyncStatus;
import com.teggr.notebook.service.GitService;
import com.teggr.notebook.service.SettingsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/git")
public class GitApiController {

    private final GitService gitService;
    private final SettingsService settingsService;

    public GitApiController(GitService gitService, SettingsService settingsService) {
        this.gitService = gitService;
        this.settingsService = settingsService;
    }

    @PostMapping("/sync")
    public SyncStatus sync() {
        String remoteUrl = settingsService.getRemoteUrl();
        String token = settingsService.getToken();
        return gitService.sync(remoteUrl, token);
    }
}
