package com.teggr.notebook.controller;

import com.teggr.notebook.model.Note;
import com.teggr.notebook.model.NoteListItem;
import com.teggr.notebook.service.NoteService;
import org.springframework.core.io.PathResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
public class NoteApiController {

    private final NoteService noteService;

    public NoteApiController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public List<NoteListItem> listNotes() {
        return noteService.listNotes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Note> getNote(@PathVariable String id) {
        return noteService.getNote(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Note> createNote(@RequestBody Map<String, String> body) throws IOException {
        String title = body.getOrDefault("title", "Untitled");
        String content = body.getOrDefault("content", "# " + title + "\n\n");
        Note note = noteService.createNote(title, content);
        return ResponseEntity.status(HttpStatus.CREATED).body(note);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Note> updateNote(@PathVariable String id, @RequestBody Map<String, String> body) throws IOException {
        String content = body.getOrDefault("content", "");
        Note note = noteService.updateNote(id, content);
        return ResponseEntity.ok(note);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable String id) throws IOException {
        noteService.deleteNote(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/images/{filename}")
    public ResponseEntity<PathResource> getImage(@PathVariable String filename) {
        Path imagePath = noteService.getNotesDir().resolve("images").resolve(filename);
        if (!Files.exists(imagePath)) return ResponseEntity.notFound().build();
        String contentType = "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) contentType = "image/jpeg";
        else if (filename.endsWith(".gif")) contentType = "image/gif";
        else if (filename.endsWith(".webp")) contentType = "image/webp";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(new PathResource(imagePath));
    }
}
