package com.teggr.notebook.template;

import com.teggr.notebook.model.Note;
import com.teggr.notebook.model.NoteListItem;
import j2html.tags.specialized.HtmlTag;

import java.util.List;

import static j2html.TagCreator.*;

public class HtmlTemplates {

    public static String mainPage(List<NoteListItem> notes, Note selectedNote) {
        String selectedId = selectedNote != null ? selectedNote.getId() : (notes.isEmpty() ? "" : notes.get(0).getId());
        String selectedContent = selectedNote != null ? selectedNote.getContent() : "";

        return document(
            html(
                head(
                    meta().withCharset("UTF-8"),
                    meta().withName("viewport").withContent("width=device-width, initial-scale=1.0"),
                    title("Notebook"),
                    link().withRel("stylesheet").withHref("/vendor/easymde.min.css"),
                    link().withRel("stylesheet").withHref("/css/app.css")
                ),
                body(
                    div().withClass("app").with(
                        // Left panel
                        div().withClass("sidebar").with(
                            div().withClass("sidebar-header").with(
                                span("Notes"),
                                button("+ New").withClass("btn-new").withId("btn-new")
                            ),
                            div().withClass("note-list").withId("note-list").with(
                                each(notes, note ->
                                    div().withClass("note-item" + (note.getId().equals(selectedId) ? " active" : ""))
                                         .withData("id", note.getId())
                                         .with(
                                             div(note.getTitle()).withClass("note-title"),
                                             div(note.getLastModifiedFormatted()).withClass("note-date")
                                         )
                                )
                            )
                        ),
                        // Right panel
                        div().withClass("editor-panel").with(
                            div().withClass("editor-toolbar-custom").with(
                                span().withClass("save-status").withId("save-status"),
                                button("Sync").withClass("btn-sync").withId("btn-sync"),
                                button("Delete").withClass("btn-delete").withId("btn-delete")
                            ),
                            textarea().withId("editor").withName("content").withText(selectedContent)
                        )
                    ),
                    // Hidden field for current note id
                    input().withType("hidden").withId("current-note-id").withValue(selectedId),
                    script().withSrc("/vendor/easymde.min.js"),
                    script().withSrc("/js/app.js")
                )
            )
        );
    }

    public static String settingsPage(String remoteUrl, String token, String message) {
        return document(
            html(
                head(
                    meta().withCharset("UTF-8"),
                    title("Settings - Notebook"),
                    link().withRel("stylesheet").withHref("/css/app.css")
                ),
                body(
                    div().withClass("settings-page").with(
                        h1("Settings"),
                        message != null && !message.isEmpty() ? p(message).withClass("message") : span(),
                        form().withMethod("POST").withAction("/settings").with(
                            div().withClass("form-group").with(
                                label("GitHub Repository URL").withFor("remoteUrl"),
                                input().withType("text").withId("remoteUrl").withName("remoteUrl")
                                       .withValue(remoteUrl != null ? remoteUrl : "").withPlaceholder("https://github.com/user/repo.git")
                            ),
                            div().withClass("form-group").with(
                                label("GitHub Personal Access Token").withFor("token"),
                                input().withType("password").withId("token").withName("token")
                                       .withValue(token != null ? token : "").withPlaceholder("ghp_...")
                            ),
                            button("Save Settings").withType("submit").withClass("btn-save")
                        ),
                        a("← Back to Notes").withHref("/").withClass("back-link")
                    )
                )
            )
        );
    }
}
