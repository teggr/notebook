package com.teggr.notebook.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notebook")
public class NotebookProperties {
    private String notesDir = System.getProperty("user.home") + "/notebook/notes";

    public String getNotesDir() { return notesDir; }
    public void setNotesDir(String notesDir) { this.notesDir = notesDir; }
}
