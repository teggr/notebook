package com.teggr.notebook.service;

import com.teggr.notebook.model.SyncStatus;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class GitService {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    private Path notesDir;

    public void initIfNeeded(Path notesDir) {
        this.notesDir = notesDir;
        File gitDir = notesDir.resolve(".git").toFile();
        if (!gitDir.exists()) {
            try (Git git = Git.init().setDirectory(notesDir.toFile()).call()) {
                // initialized
            } catch (GitAPIException e) {
                // ignore
            }
        }
    }

    public void commitAsync(String message, Path dir) {
        executor.submit(() -> {
            try {
                doCommit(message, dir);
            } catch (Exception e) {
                // log but don't fail
            }
        });
    }

    private void doCommit(String message, Path dir) throws IOException, GitAPIException {
        try (Git git = Git.open(dir.toFile())) {
            git.add().addFilepattern(".").call();
            var status = git.status().call();
            if (!status.isClean()) {
                git.commit()
                   .setMessage(message)
                   .setAuthor("Notebook", "notebook@localhost")
                   .call();
            }
        }
    }

    public SyncStatus sync(String remoteUrl, String token) {
        if (notesDir == null) return new SyncStatus("error", "Notes directory not initialized");
        try (Git git = Git.open(notesDir.toFile())) {
            UsernamePasswordCredentialsProvider creds = null;
            if (token != null && !token.isBlank()) {
                creds = new UsernamePasswordCredentialsProvider(token, "");
            }
            // pull
            var pullCmd = git.pull();
            if (creds != null) pullCmd.setCredentialsProvider(creds);
            try {
                pullCmd.call();
            } catch (Exception e) {
                // no remote or other issue - continue
            }
            // push
            var pushCmd = git.push();
            if (creds != null) pushCmd.setCredentialsProvider(creds);
            try {
                pushCmd.call();
                return new SyncStatus("ok", "Synced successfully");
            } catch (Exception e) {
                return new SyncStatus("error", "Push failed: " + e.getMessage());
            }
        } catch (IOException e) {
            return new SyncStatus("error", "Git error: " + e.getMessage());
        }
    }
}
