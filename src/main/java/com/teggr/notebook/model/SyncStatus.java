package com.teggr.notebook.model;

public class SyncStatus {
    private String status;
    private String message;

    public SyncStatus(String status, String message) {
        this.status = status; this.message = message;
    }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
}
