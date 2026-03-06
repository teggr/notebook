package com.teggr.notebook.service;

import com.teggr.notebook.model.SyncStatus;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

@Service
public class GitService {

    private static final Logger log = LoggerFactory.getLogger(GitService.class);
    private static final long SYNC_COMMIT_WAIT_TIMEOUT_MS = 5000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Future<?>> pendingCommits = Collections.synchronizedList(new ArrayList<>());
    private Path notesDir;

    public void shutdown() {
        waitForPendingCommits(SYNC_COMMIT_WAIT_TIMEOUT_MS);
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void initIfNeeded(Path notesDir) {
        this.notesDir = notesDir;
        File gitDir = notesDir.resolve(".git").toFile();
        if (!gitDir.exists()) {
            try (Git git = Git.init().setDirectory(notesDir.toFile()).setInitialBranch("main").call()) {
                log.info("Initialized git repository at {}", notesDir);
            } catch (GitAPIException e) {
                log.warn("Failed to initialize git repository at {}: {}", notesDir, e.getMessage());
            }
        }
    }

    public void commitAsync(String message, Path dir) {
        synchronized (pendingCommits) {
            Future<?> future = executor.submit(() -> {
                try {
                    doCommit(message, dir);
                } catch (Exception e) {
                    log.warn("Failed to commit '{}': {}", message, e.getMessage());
                }
            });
            pendingCommits.add(future);
        }
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
        if (remoteUrl == null || remoteUrl.isBlank()) {
            return new SyncStatus("error", "Remote URL not configured");
        }

        // Avoid racing sync against queued async commits from note updates.
        waitForPendingCommits(SYNC_COMMIT_WAIT_TIMEOUT_MS);

        try (Git git = Git.open(notesDir.toFile())) {
            // Configure remote origin
            try {
                var config = git.getRepository().getConfig();
                String existingUrl = config.getString("remote", "origin", "url");
                if (existingUrl == null || !existingUrl.equals(remoteUrl)) {
                    config.setString("remote", "origin", "url", remoteUrl);
                    config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
                    config.save();
                    log.info("Configured remote origin URL: {}", remoteUrl);
                }
            } catch (Exception e) {
                return new SyncStatus("error", "Failed to configure remote: " + e.getMessage());
            }
            
            UsernamePasswordCredentialsProvider creds = null;
            if (token != null && !token.isBlank()) {
                // For GitHub PAT, use it as the password with a placeholder username
                creds = new UsernamePasswordCredentialsProvider("x-access-token", token);
            }

            alignBranchWithRemoteIfNeeded(git, creds);

            String currentBranch = git.getRepository().getBranch();
            boolean remoteHasHeads = hasAnyRemoteHead(git, creds);

            if (remoteHasHeads) {
                var pullCmd = git.pull()
                        .setRemote("origin")
                        .setRemoteBranchName(currentBranch);
                if (creds != null) pullCmd.setCredentialsProvider(creds);
                try {
                    pullCmd.call();
                } catch (Exception e) {
                    log.warn("Pull failed: {}", e.getMessage());
                }
            } else {
                log.info("Skipping pull because remote origin has no branches yet");
            }

            ObjectId head = git.getRepository().resolve("HEAD");
            if (head == null) {
                return new SyncStatus("error", "No local commits to push yet. Create or edit a note, then sync again.");
            }

            // push
            var pushCmd = git.push()
                    .setRemote("origin")
                    .setRefSpecs(new RefSpec("refs/heads/" + currentBranch + ":refs/heads/" + currentBranch));
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

    // Package-private for testing.
    void waitForPendingCommits(long timeoutMillis) {
        if (timeoutMillis <= 0) {
            return;
        }

        final long deadline = System.currentTimeMillis() + timeoutMillis;

        while (true) {
            long now = System.currentTimeMillis();
            long remainingMs = deadline - now;
            if (remainingMs <= 0) {
                break;
            }

            final List<Future<?>> snapshot;
            synchronized (pendingCommits) {
                if (pendingCommits.isEmpty()) {
                    // No more commits currently pending; we're done.
                    break;
                }
                snapshot = new ArrayList<>(pendingCommits);
                pendingCommits.clear();
            }

            for (Future<?> future : snapshot) {
                remainingMs = deadline - System.currentTimeMillis();
                if (remainingMs <= 0) {
                    // Timeout reached; stop waiting.
                    break;
                }
                try {
                    future.get(remainingMs, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Pending commit wait was interrupted before sync: {}", e.getMessage());
                    return;
                } catch (TimeoutException e) {
                    log.warn("Pending commit timed out before sync: {}", e.getMessage());
                } catch (ExecutionException e) {
                    log.warn("Pending commit did not complete before sync: {}", e.getMessage());
                }
            }
        }
    }

    private boolean hasAnyRemoteHead(Git git, UsernamePasswordCredentialsProvider creds) {
        try {
            var lsRemote = git.lsRemote()
                    .setRemote("origin")
                    .setHeads(true);
            if (creds != null) lsRemote.setCredentialsProvider(creds);
            return !lsRemote.call().isEmpty();
        } catch (Exception e) {
            log.warn("Could not inspect remote heads: {}", e.getMessage());
            return true;
        }
    }

    private void alignBranchWithRemoteIfNeeded(Git git, UsernamePasswordCredentialsProvider creds) {
        try {
            var fetch = git.fetch()
                    .setRemote("origin")
                    .setRefSpecs(new RefSpec("+refs/heads/*:refs/remotes/origin/*"));
            if (creds != null) fetch.setCredentialsProvider(creds);
            fetch.call();

            String currentBranch = git.getRepository().getBranch();
            var remoteMain = git.getRepository().findRef("refs/remotes/origin/main");
            var remoteMaster = git.getRepository().findRef("refs/remotes/origin/master");

            if ("master".equals(currentBranch) && remoteMain != null && remoteMaster == null) {
                var localMain = git.getRepository().findRef("refs/heads/main");
                if (localMain == null) {
                    git.branchRename().setOldName("master").setNewName("main").call();
                } else {
                    git.checkout().setName("main").call();
                }

                var config = git.getRepository().getConfig();
                config.setString("branch", "main", "remote", "origin");
                config.setString("branch", "main", "merge", "refs/heads/main");
                config.unsetSection("branch", "master");
                config.save();

                log.info("Aligned local branch from master to main to match remote");
            }
        } catch (Exception e) {
            log.warn("Could not align local branch with remote: {}", e.getMessage());
        }
    }
}
