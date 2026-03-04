# notebook-api

A git-native REST API for managing markdown notes. Notes are stored as `.md` files on disk and automatically committed to a local git repository. Optionally sync with a remote GitHub repository.

## Running

```bash
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080`.

Configuration is read from `application.properties`:

| Property | Default | Description |
|---|---|---|
| `notebook.notes-dir` | `~/notebook/notes` | Directory where notes are stored |
| `server.port` | `8080` | Port the API listens on |

## API Reference

### Notes

#### List notes

```
GET /api/notes
```

Returns a list of all notes sorted by last-modified date (newest first).

**Response** `200 OK`
```json
[
  {
    "id": "My-Note",
    "title": "My Note",
    "lastModifiedFormatted": "Mar 4, 2026"
  }
]
```

---

#### Get a note

```
GET /api/notes/{id}
```

**Response** `200 OK`
```json
{
  "id": "My-Note",
  "title": "My Note",
  "content": "# My Note\n\nNote content here.",
  "lastModified": "2026-03-04T15:00:00Z"
}
```

Returns `404 Not Found` if the note does not exist.

---

#### Create a note

```
POST /api/notes
Content-Type: application/json
```

**Request body**
```json
{
  "title": "My Note",
  "content": "# My Note\n\nNote content here."
}
```

Both fields are optional. `title` defaults to `"Untitled"`. `content` defaults to `"# {title}\n\n"`.

**Response** `201 Created`
```json
{
  "id": "My-Note",
  "title": "My Note",
  "content": "# My Note\n\nNote content here.",
  "lastModified": "2026-03-04T15:00:00Z"
}
```

---

#### Update a note

```
PUT /api/notes/{id}
Content-Type: application/json
```

**Request body**
```json
{
  "content": "# My Note\n\nUpdated content."
}
```

**Response** `200 OK`
```json
{
  "id": "My-Note",
  "title": "My Note",
  "content": "# My Note\n\nUpdated content.",
  "lastModified": "2026-03-04T15:01:00Z"
}
```

---

#### Delete a note

```
DELETE /api/notes/{id}
```

**Response** `204 No Content`

---

### Images

#### Upload an image

```
POST /api/images
Content-Type: multipart/form-data
```

| Part | Description |
|---|---|
| `file` | Image file (`png`, `jpg`, `jpeg`, `gif`, `webp`) |

**Response** `200 OK`
```json
{
  "markdown": "![](images/a1b2c3d4-....png)"
}
```

Returns the markdown snippet to embed the image in a note.

---

#### Get an image

```
GET /api/notes/images/{filename}
```

Returns the image file with the appropriate `Content-Type` header.

Returns `404 Not Found` if the image does not exist.

---

### Git / Sync

#### Sync with remote

```
POST /api/git/sync
```

Pulls from and pushes to the configured remote repository using the stored credentials.

**Response** `200 OK`
```json
{
  "status": "ok",
  "message": "Synced successfully"
}
```

On failure the `status` field is `"error"` and `message` contains the reason.

---

### Settings

#### Get settings

```
GET /api/settings
```

**Response** `200 OK`
```json
{
  "remoteUrl": "https://github.com/user/repo.git",
  "token": "ghp_..."
}
```

---

#### Update settings

```
PUT /api/settings
Content-Type: application/json
```

**Request body**
```json
{
  "remoteUrl": "https://github.com/user/repo.git",
  "token": "ghp_..."
}
```

**Response** `200 OK`
```json
{
  "remoteUrl": "https://github.com/user/repo.git",
  "token": "ghp_..."
}
```

Settings are persisted to `~/.notebook/config.properties`.
