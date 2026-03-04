# Copilot Instructions

## Project Overview

`notebook-api` is a Spring Boot REST API for managing git-native markdown notes. It is the API backend half of the notebook application — there is no UI code in this repository.

Notes are stored as `.md` files on disk and automatically committed to a local git repository (via JGit). The API supports optional sync with a remote GitHub repository using a personal access token.

## Technology Stack

- **Java 17** + **Spring Boot 3** (`spring-boot-starter-web`)
- **JGit** (`org.eclipse.jgit`) for local git operations and remote sync
- **Maven** for build and dependency management

## Project Structure

```
src/main/java/com/teggr/notebook/
├── NotebookApplication.java          # Spring Boot entry point
├── config/
│   └── NotebookProperties.java       # @ConfigurationProperties (notes-dir)
├── controller/
│   ├── NoteApiController.java        # REST: /api/notes/**
│   ├── ImageController.java          # REST: /api/images (upload)
│   ├── GitApiController.java         # REST: /api/git/sync
│   └── SettingsApiController.java    # REST: /api/settings
├── model/
│   ├── Note.java                     # Full note (id, title, content, lastModified)
│   ├── NoteListItem.java             # Summary for list view
│   ├── Settings.java                 # Git remote URL and token
│   └── SyncStatus.java              # Git sync result (status, message)
└── service/
    ├── NoteService.java              # CRUD for notes (reads/writes .md files)
    ├── GitService.java               # JGit operations (init, commit, sync)
    └── SettingsService.java          # Persist settings in ~/.notebook/config.properties
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/notes` | List all notes |
| GET | `/api/notes/{id}` | Get a note by ID |
| POST | `/api/notes` | Create a new note |
| PUT | `/api/notes/{id}` | Update a note |
| DELETE | `/api/notes/{id}` | Delete a note |
| GET | `/api/notes/images/{filename}` | Serve an image |
| POST | `/api/images` | Upload an image |
| POST | `/api/git/sync` | Sync with remote git repository |
| GET | `/api/settings` | Get git remote settings |
| PUT | `/api/settings` | Update git remote settings |

## Key Design Decisions

- All controllers use `@RestController` — no server-side HTML rendering.
- Note IDs are URL-encoded filenames (without the `.md` extension).
- Notes are committed to git asynchronously after each create/update.
- Settings (remote URL, token) are stored in `~/.notebook/config.properties`.
- The notes directory is configurable via `notebook.notes-dir` in `application.properties`.

## Running & Testing

```bash
# Run
./mvnw spring-boot:run

# Test
./mvnw test
```
