package com.teggr.notebook.service;

import com.teggr.notebook.model.SyncStatus;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitServiceTest {

    @TempDir
    Path tempDir;

    private GitService gitService;
    private Path notesDir;
    private Path remoteDir;

    @BeforeEach
    void setUp() throws Exception {
        notesDir = tempDir.resolve("notes");
        Files.createDirectories(notesDir);

        remoteDir = tempDir.resolve("remote.git");
        try (Git ignored = Git.init().setBare(true).setDirectory(remoteDir.toFile()).call()) {
            // Bare remote initialized for local sync tests.
        }

        gitService = new GitService();
        gitService.initIfNeeded(notesDir);
    }

    @AfterEach
    void tearDown() {
        gitService.shutdown();
    }

    @Test
    void syncReturnsErrorWhenNoLocalCommitsExist() {
        SyncStatus status = gitService.sync(remoteDir.toUri().toString(), null);

        assertEquals("error", status.getStatus());
        assertTrue(status.getMessage().contains("No local commits to push yet"));
    }

    @Test
    void waitForPendingCommitsCompletesQueuedCommit() throws Exception {
        Path noteFile = notesDir.resolve("queued.md");
        Files.writeString(noteFile, "# Queued\n\ncontent");

        gitService.commitAsync("Queued commit", notesDir);
        gitService.waitForPendingCommits(5000);

        try (Git git = Git.open(notesDir.toFile())) {
            assertNotNull(git.getRepository().resolve("HEAD"));
        }
    }

    @Test
    void syncWaitsForQueuedCommitAndPushesToRemote() throws Exception {
        Path noteFile = notesDir.resolve("sync.md");
        Files.writeString(noteFile, "# Sync\n\ncontent");

        gitService.commitAsync("Commit before sync", notesDir);

        SyncStatus status = gitService.sync(remoteDir.toUri().toString(), null);

        assertEquals("ok", status.getStatus());

        try (Git remoteGit = Git.open(remoteDir.toFile())) {
            assertNotNull(remoteGit.getRepository().findRef("refs/heads/main"));
        }
    }

    @Test
    void waitForPendingCommitsReturnsAfterTimeoutForStuckFuture() throws Exception {
        Field pendingField = GitService.class.getDeclaredField("pendingCommits");
        pendingField.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Future<?>> pending = (List<Future<?>>) pendingField.get(gitService);
        pending.add(new CompletableFuture<>());

        long startNanos = System.nanoTime();
        gitService.waitForPendingCommits(25);
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        assertTrue(elapsedMs < 500, "waitForPendingCommits should return near configured timeout");
    }
}
