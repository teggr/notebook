package com.teggr.notebook.controller;

import com.teggr.notebook.model.Note;
import com.teggr.notebook.model.NoteListItem;
import com.teggr.notebook.service.NoteService;
import com.teggr.notebook.template.HtmlTemplates;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class MainController {

    private final NoteService noteService;

    public MainController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String index() {
        List<NoteListItem> notes = noteService.listNotes();
        Note selected = notes.isEmpty() ? null : noteService.getNote(notes.get(0).getId()).orElse(null);
        return HtmlTemplates.mainPage(notes, selected);
    }

    @GetMapping(value = "/note/{id}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String viewNote(@PathVariable String id) {
        List<NoteListItem> notes = noteService.listNotes();
        Note selected = noteService.getNote(id).orElse(null);
        return HtmlTemplates.mainPage(notes, selected);
    }
}
