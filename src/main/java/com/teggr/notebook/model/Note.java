package com.teggr.notebook.model;

import java.time.Instant;

public class Note {
    private String id;
    private String title;
    private String content;
    private Instant lastModified;

    public Note() {}
    public Note(String id, String title, String content, Instant lastModified) {
        this.id = id; this.title = title; this.content = content; this.lastModified = lastModified;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public java.time.Instant getLastModified() { return lastModified; }
    public void setLastModified(java.time.Instant lastModified) { this.lastModified = lastModified; }
}
