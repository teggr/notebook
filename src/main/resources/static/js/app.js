let easyMDE;
let currentNoteId = document.getElementById('current-note-id')?.value || '';
let saveTimeout;
let isSaving = false;

// Replicates NoteService.toId logic to compute note ID from title
function titleToId(title) {
    const sanitized = title.trim().replace(/[^a-zA-Z0-9 _-]/g, '').trim().replace(/\s+/g, '-');
    return encodeURIComponent(sanitized || 'untitled');
}

function renderWikiLinks(html) {
    return html.replace(/\[\[([^\]]+)\]\]/g, function(match, title) {
        const id = titleToId(title);
        return `<a href="/note/${id}" class="wiki-link">${title} <span class="wiki-link-arrow" aria-hidden="true">↗</span></a>`;
    });
}

function updateWikiNavButton(cm) {
    const btn = document.getElementById('btn-wiki-nav');
    if (!btn) return;
    const pos = cm.getCursor();
    const line = cm.getLine(pos.line);
    if (!line) { btn.style.display = 'none'; return; }
    const wikiLinkRegex = /\[\[([^\]]+)\]\]/g;
    let match;
    while ((match = wikiLinkRegex.exec(line)) !== null) {
        if (pos.ch >= match.index && pos.ch <= match.index + match[0].length) {
            btn.dataset.title = match[1];
            btn.style.display = '';
            return;
        }
    }
    btn.style.display = 'none';
}

function initEditor() {
    const textarea = document.getElementById('editor');
    if (!textarea) return;

    easyMDE = new EasyMDE({
        element: textarea,
        autosave: { enabled: false },
        spellChecker: false,
        sideBySideFullscreen: false,
        previewRender: function(text) {
            let html = easyMDE.markdown(text);
            return renderWikiLinks(html);
        },
        toolbar: [
            'bold', 'italic', '|',
            'heading-1', 'heading-2', 'heading-3',
            {
                name: 'heading-4',
                action: function(editor) {
                    const cm = editor.codemirror;
                    const cursor = cm.getCursor();
                    const line = cm.getLine(cursor.line);
                    const stripped = line.replace(/^#{1,6} /, '');
                    const newLine = line.startsWith('#### ') ? stripped : '#### ' + stripped;
                    cm.replaceRange(newLine, {line: cursor.line, ch: 0}, {line: cursor.line, ch: line.length});
                },
                className: 'fa fa-heading heading-4',
                title: 'Heading 4'
            },
            '|',
            'quote', 'unordered-list', 'ordered-list', '|',
            'link', 'image', '|',
            'preview', 'side-by-side', 'fullscreen', '|',
            'guide',
            {
                name: 'settings',
                action: function() { window.location.href = '/settings'; },
                className: 'fa fa-cog',
                title: 'Settings'
            }
        ]
    });

    easyMDE.codemirror.on('change', function() {
        scheduleSave();
    });

    // Wiki-link overlay: highlight [[title]] in the editor
    const wikiLinkOverlay = {
        token: function(stream) {
            if (stream.match(/\[\[[^\]]+\]\]/)) {
                return 'wiki-link';
            }
            while (stream.next() !== null && !stream.match(/\[\[/, false)) {}
            return null;
        }
    };
    easyMDE.codemirror.addOverlay(wikiLinkOverlay);

    // Show wiki nav button when cursor is inside [[title]]
    easyMDE.codemirror.on('cursorActivity', function(cm) {
        updateWikiNavButton(cm);
    });

    // Image paste support
    easyMDE.codemirror.on('paste', function(cm, e) {
        const items = e.clipboardData && e.clipboardData.items;
        if (!items) return;
        for (let i = 0; i < items.length; i++) {
            if (items[i].type.startsWith('image/')) {
                e.preventDefault();
                const blob = items[i].getAsFile();
                uploadImage(blob, cm);
                break;
            }
        }
    });
}

function uploadImage(blob, cm) {
    const formData = new FormData();
    formData.append('file', blob, 'pasted-image.png');
    fetch('/api/images', { method: 'POST', body: formData })
        .then(r => r.json())
        .then(data => {
            const cursor = cm.getCursor();
            cm.replaceRange(data.markdown, cursor);
        })
        .catch(err => console.error('Image upload failed:', err));
}

function scheduleSave() {
    clearTimeout(saveTimeout);
    document.getElementById('save-status').textContent = 'Unsaved...';
    saveTimeout = setTimeout(saveCurrentNote, 2000);
}

function saveCurrentNote() {
    if (!currentNoteId || isSaving) return;
    isSaving = true;
    const content = easyMDE.value();
    document.getElementById('save-status').textContent = 'Saving...';
    fetch(`/api/notes/${currentNoteId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content })
    })
    .then(r => r.json())
    .then(note => {
        document.getElementById('save-status').textContent = 'Saved';
        updateNoteInList(note);
        setTimeout(() => {
            const status = document.getElementById('save-status');
            if (status) status.textContent = '';
        }, 2000);
    })
    .catch(err => {
        document.getElementById('save-status').textContent = 'Save failed';
        console.error(err);
    })
    .finally(() => { isSaving = false; });
}

function updateNoteInList(note) {
    const items = document.querySelectorAll('.note-item');
    items.forEach(item => {
        if (item.dataset.id === note.id) {
            const titleEl = item.querySelector('.note-title');
            if (titleEl) titleEl.textContent = note.title;
        }
    });
}

function loadNote(id) {
    if (id === currentNoteId) return;
    // Save current before switching
    if (currentNoteId && easyMDE) {
        clearTimeout(saveTimeout);
        saveCurrentNote();
    }
    fetch(`/api/notes/${id}`)
        .then(r => r.json())
        .then(note => {
            currentNoteId = note.id;
            document.getElementById('current-note-id').value = note.id;
            easyMDE.value(note.content);
            // Update active state
            document.querySelectorAll('.note-item').forEach(item => {
                item.classList.toggle('active', item.dataset.id === note.id);
            });
            history.pushState(null, '', `/note/${note.id}`);
        })
        .catch(err => console.error('Failed to load note:', err));
}

function createNote() {
    const title = prompt('Note title:', 'New Note');
    if (!title) return;
    fetch('/api/notes', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title, content: '# ' + title + '\n\n' })
    })
    .then(r => r.json())
    .then(note => {
        // Reload the page to show updated list
        window.location.href = `/note/${note.id}`;
    })
    .catch(err => console.error('Failed to create note:', err));
}

function deleteCurrentNote() {
    if (!currentNoteId) return;
    if (!confirm('Delete this note?')) return;
    fetch(`/api/notes/${currentNoteId}`, { method: 'DELETE' })
        .then(() => { window.location.href = '/'; })
        .catch(err => console.error('Failed to delete note:', err));
}

function showToast(message, isError) {
    let toast = document.getElementById('toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'toast';
        document.body.appendChild(toast);
    }
    toast.textContent = message;
    toast.className = 'toast' + (isError ? ' toast-error' : ' toast-ok');
    toast.setAttribute('role', 'status');
    toast.setAttribute('aria-live', 'polite');
    clearTimeout(toast._hideTimer);
    toast._hideTimer = setTimeout(() => { toast.className = 'toast'; }, 4000);
}

function syncNotes() {
    const btn = document.getElementById('btn-sync');
    btn.textContent = 'Syncing...';
    btn.disabled = true;
    fetch('/api/git/sync', { method: 'POST' })
        .then(r => r.json())
        .then(result => {
            showToast(result.message || result.status, result.status === 'error');
        })
        .catch(err => showToast('Sync failed: ' + err, true))
        .finally(() => {
            btn.textContent = 'Sync';
            btn.disabled = false;
        });
}

// Event listeners
document.addEventListener('DOMContentLoaded', function() {
    initEditor();

    document.getElementById('note-list')?.addEventListener('click', function(e) {
        const item = e.target.closest('.note-item');
        if (item) loadNote(item.dataset.id);
    });

    document.getElementById('btn-new')?.addEventListener('click', createNote);
    document.getElementById('btn-delete')?.addEventListener('click', deleteCurrentNote);
    document.getElementById('btn-sync')?.addEventListener('click', syncNotes);
    document.getElementById('btn-wiki-nav')?.addEventListener('click', function() {
        const title = this.dataset.title;
        if (title) window.location.href = '/note/' + titleToId(title);
    });
});
