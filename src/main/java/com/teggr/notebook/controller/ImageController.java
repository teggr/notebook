package com.teggr.notebook.controller;

import com.teggr.notebook.service.NoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
public class ImageController {

    private final NoteService noteService;

    public ImageController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping("/api/images")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        String ext = "png";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
        }
        String filename = UUID.randomUUID() + "." + ext;
        Path imageDir = noteService.getNotesDir().resolve("images");
        Files.createDirectories(imageDir);
        Files.copy(file.getInputStream(), imageDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        return ResponseEntity.ok(Map.of("markdown", "![](images/" + filename + ")"));
    }
}
