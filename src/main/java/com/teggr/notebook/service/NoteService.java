package com.teggr.notebook.service;

import com.teggr.notebook.config.NotebookProperties;
import com.teggr.notebook.model.Note;
import com.teggr.notebook.model.NoteListItem;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NoteService {

    private final Path notesDir;
    private final GitService gitService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault());

    public NoteService(NotebookProperties props, GitService gitService) {
        this.notesDir = Path.of(props.getNotesDir());
        this.gitService = gitService;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(notesDir);
        Files.createDirectories(notesDir.resolve("images"));
        gitService.initIfNeeded(notesDir);
    }

    public List<NoteListItem> listNotes() {
        try {
            return Files.list(notesDir)
                    .filter(p -> p.toString().endsWith(".md"))
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                        } catch (IOException e) { return 0; }
                    })
                    .map(p -> {
                        try {
                            String content = Files.readString(p);
                            String title = extractTitle(content);
                            String id = toId(p.getFileName().toString().replace(".md", ""));
                            Instant lastMod = Files.getLastModifiedTime(p).toInstant();
                            return new NoteListItem(id, title, FORMATTER.format(lastMod));
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public Optional<Note> getNote(String id) {
        Path file = notesDir.resolve(decodeId(id) + ".md");
        if (!Files.exists(file)) return Optional.empty();
        try {
            String content = Files.readString(file);
            String title = extractTitle(content);
            Instant lastMod = Files.getLastModifiedTime(file).toInstant();
            return Optional.of(new Note(id, title, content, lastMod));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public Note createNote(String title, String content) throws IOException {
        String id = toId(title);
        Path file = notesDir.resolve(decodeId(id) + ".md");
        Files.writeString(file, content);
        Instant lastMod = Files.getLastModifiedTime(file).toInstant();
        gitService.commitAsync("Create: " + title, notesDir);
        return new Note(id, title, content, lastMod);
    }

    public Note updateNote(String id, String content) throws IOException {
        Path file = notesDir.resolve(decodeId(id) + ".md");
        Files.writeString(file, content);
        String title = extractTitle(content);
        Instant lastMod = Files.getLastModifiedTime(file).toInstant();
        gitService.commitAsync("Update: " + title, notesDir);
        return new Note(id, title, content, lastMod);
    }

    public void deleteNote(String id) throws IOException {
        Path file = notesDir.resolve(decodeId(id) + ".md");
        Files.deleteIfExists(file);
    }

    public String extractTitle(String content) {
        if (content == null || content.isBlank()) return "Untitled";
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("# ")) return line.substring(2).trim();
            if (!line.isEmpty()) return line;
        }
        return "Untitled";
    }

    public String toId(String title) {
        String sanitized = title.replaceAll("[^a-zA-Z0-9 _-]", "").trim().replaceAll("\\s+", "-");
        if (sanitized.isEmpty()) sanitized = UUID.randomUUID().toString();
        return URLEncoder.encode(sanitized, StandardCharsets.UTF_8);
    }

    private String decodeId(String id) {
        return URLDecoder.decode(id, StandardCharsets.UTF_8);
    }

    public Path getNotesDir() {
        return notesDir;
    }
}
