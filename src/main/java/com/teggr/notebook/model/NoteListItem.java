package com.teggr.notebook.model;

public class NoteListItem {
    private String id;
    private String title;
    private String lastModifiedFormatted;

    public NoteListItem(String id, String title, String lastModifiedFormatted) {
        this.id = id; this.title = title; this.lastModifiedFormatted = lastModifiedFormatted;
    }
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getLastModifiedFormatted() { return lastModifiedFormatted; }
}
