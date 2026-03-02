# Notebook - Feature List

A git-native, markdown-based note-taking application built with Spring Boot.

## Features

### Note Management
- Notes stored as `.md` files in `~/notebook/notes/` (configurable)
- Note titles extracted from first `# H1` heading or first line
- List notes sorted by last modified date (most recent first)
- Full CRUD: create, read, update, delete notes

### Two-Panel UI
- Left sidebar: scrollable list of notes with title + modified date
- Right panel: EasyMDE markdown editor
- Selected note highlighted in sidebar
- Auto-save with 2-second debounce

### Image Paste
- Paste image from clipboard → stored in `notes/images/`
- Automatically inserts `![](images/filename.png)` at cursor
- Endpoint: `POST /api/images`

### Wiki Links
- Syntax: `[[Note Title]]`
- Rendered as clickable links in preview mode
- Navigate to linked notes

### Git Integration
- Notes directory auto-initialized as git repo
- Auto-commit on every note save
- Manual sync (push/pull) via Sync button
- GitHub token-based authentication

### Settings
- Configure GitHub repository URL
- Store GitHub Personal Access Token
- Settings persisted in `~/.notebook/config.properties`

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Main page |
| GET | `/note/{id}` | View specific note |
| GET | `/api/notes` | List all notes (JSON) |
| GET | `/api/notes/{id}` | Get note content |
| POST | `/api/notes` | Create note |
| PUT | `/api/notes/{id}` | Update note |
| DELETE | `/api/notes/{id}` | Delete note |
| GET | `/api/notes/images/{filename}` | Serve image |
| POST | `/api/images` | Upload image |
| POST | `/api/git/sync` | Sync with remote |
| GET | `/settings` | Settings page |
| POST | `/settings` | Save settings |
