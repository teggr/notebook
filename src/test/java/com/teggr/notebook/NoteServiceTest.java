package com.teggr.notebook;

import com.teggr.notebook.config.NotebookProperties;
import com.teggr.notebook.model.Note;
import com.teggr.notebook.model.NoteListItem;
import com.teggr.notebook.service.GitService;
import com.teggr.notebook.service.NoteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NoteServiceTest {

    @TempDir
    Path tempDir;

    NoteService noteService;
    GitService gitService;

    @BeforeEach
    void setUp() throws IOException {
        NotebookProperties props = new NotebookProperties();
        props.setNotesDir(tempDir.toString());
        gitService = new GitService();
        noteService = new NoteService(props, gitService);
        noteService.init();
    }

    @AfterEach
    void tearDown() {
        gitService.shutdown();
    }

    @Test
    void extractTitleFromH1() {
        assertEquals("My Note", noteService.extractTitle("# My Note\n\nContent here."));
    }

    @Test
    void extractTitleFromFirstLine() {
        assertEquals("My Note", noteService.extractTitle("My Note\n\nContent here."));
    }

    @Test
    void createAndGetNote() throws IOException {
        Note created = noteService.createNote("Test Note", "# Test Note\n\nHello world");
        assertNotNull(created);
        assertEquals("Test Note", created.getTitle());

        Optional<Note> fetched = noteService.getNote(created.getId());
        assertTrue(fetched.isPresent());
        assertEquals("Test Note", fetched.get().getTitle());
    }

    @Test
    void listNotesReturnsSortedByDate() throws IOException {
        Note first = noteService.createNote("First Note", "# First Note");
        Note second = noteService.createNote("Second Note", "# Second Note");
        // Set timestamps explicitly so ordering is deterministic
        java.nio.file.attribute.FileTime earlier = java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() - 10000);
        java.nio.file.attribute.FileTime later = java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis());
        java.nio.file.Files.setLastModifiedTime(tempDir.resolve(java.net.URLDecoder.decode(first.getId(), java.nio.charset.StandardCharsets.UTF_8) + ".md"), earlier);
        java.nio.file.Files.setLastModifiedTime(tempDir.resolve(java.net.URLDecoder.decode(second.getId(), java.nio.charset.StandardCharsets.UTF_8) + ".md"), later);

        List<NoteListItem> notes = noteService.listNotes();
        assertEquals(2, notes.size());
        assertEquals("Second Note", notes.get(0).getTitle());
    }

    @Test
    void deleteNote() throws IOException {
        Note created = noteService.createNote("Delete Me", "# Delete Me");
        noteService.deleteNote(created.getId());
        assertTrue(noteService.getNote(created.getId()).isEmpty());
    }

    @Test
    void updateNote() throws IOException {
        Note created = noteService.createNote("Update Me", "# Update Me\n\nOriginal content");
        noteService.updateNote(created.getId(), "# Update Me\n\nUpdated content");
        Note updated = noteService.getNote(created.getId()).orElseThrow();
        assertTrue(updated.getContent().contains("Updated content"));
    }

    @Test
    void createDuplicateTitlesCreatesDistinctFilesWithoutOverwrite() throws IOException {
        Note first = noteService.createNote("New Note", "# Unittled\n\nFirst content");
        Note second = noteService.createNote("New Note", "# Unittled\n\nSecond content");

        assertNotEquals(first.getId(), second.getId());

        Path firstFile = tempDir.resolve(URLDecoder.decode(first.getId(), StandardCharsets.UTF_8) + ".md");
        Path secondFile = tempDir.resolve(URLDecoder.decode(second.getId(), StandardCharsets.UTF_8) + ".md");

        assertTrue(Files.exists(firstFile));
        assertTrue(Files.exists(secondFile));
        assertNotEquals(firstFile, secondFile);

        String firstContent = Files.readString(firstFile);
        String secondContent = Files.readString(secondFile);
        assertEquals("# Unittled\n\nFirst content", firstContent);
        assertEquals("# Unittled\n\nSecond content", secondContent);

        Optional<Note> firstFetched = noteService.getNote(first.getId());
        Optional<Note> secondFetched = noteService.getNote(second.getId());
        assertTrue(firstFetched.isPresent());
        assertTrue(secondFetched.isPresent());
        assertEquals("# Unittled\n\nFirst content", firstFetched.get().getContent());
        assertEquals("# Unittled\n\nSecond content", secondFetched.get().getContent());
    }
}
