package com.example.notes;

import com.example.notes.entity.Note;
import com.example.notes.request.NoteDTO;
import com.example.notes.service.NoteService;
import io.quarkus.test.InjectMock;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.equalTo;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;


public class NotesResourceTest {

    @InjectMock
    NoteService service;

    // ------------------------------------------------------------
    // POST /notes  (Create)
    // ------------------------------------------------------------
    @Test
    void testCreateNote() {
        Note input = new Note(null, "Title 1", "Content 1", System.currentTimeMillis());

        Note saved = new Note("123", "Title 1", "Content 1", System.currentTimeMillis());
        saved.setUpdatedAt(saved.getCreatedAt());

        when(service.createNote("Title 1", "Content 1"))
                .thenReturn(Uni.createFrom().item(saved));

        given()
                .contentType("application/json")
                .body(input)
                .when()
                .post("/notes")
                .then()
                .statusCode(201)
                .body("title", equalTo("Title 1"))
                .body("content", equalTo("Content 1"));
    }

    // ------------------------------------------------------------
    // GET /notes  (List all)
    // ------------------------------------------------------------
    @Test
    void testGetAllNotes() {
        Note n1 = new Note("1", "A", "B", System.currentTimeMillis());
        Note n2 = new Note("2", "C", "D", System.currentTimeMillis());

        when(service.listNotes())
                .thenReturn(Multi.createFrom().items(n1, n2));

        when()
                .get("/notes")
                .then()
                .statusCode(200)
                .body("$.size()", equalTo(2));
    }

    // ------------------------------------------------------------
    // GET /notes/{id}  (Found)
    // ------------------------------------------------------------
    @Test
    void testGetNoteById_found() {
        Note n = new Note("10", "Hello", "World", System.currentTimeMillis());

        when(service.getNote("10"))
                .thenReturn(Uni.createFrom().item(n));

        when()
                .get("/notes/10")
                .then()
                .statusCode(200)
                .body("message", equalTo("Note found!"))
                .body("data.title", equalTo("Hello"));
    }

    // ------------------------------------------------------------
    // GET /notes/{id}  (Not found)
    // ------------------------------------------------------------
    @Test
    void testGetNoteById_notFound() {
        when(service.getNote("999"))
                .thenReturn(Uni.createFrom().nullItem());

        when()
                .get("/notes/999")
                .then()
                .statusCode(404)
                .body("message", equalTo("Note not found"));
    }

    // ------------------------------------------------------------
    // GET /notes/search?title=&content=
    // ------------------------------------------------------------
    @Test
    void testSearchNotes() {
        Note match = new Note("abc", "Match", "Something", System.currentTimeMillis());

        when(service.searchNotes("Match", null))
                .thenReturn(Multi.createFrom().items(match));

        given()
                .queryParam("title", "Match")
                .when()
                .get("/notes/search")
                .then()
                .statusCode(200)
                .body("$.size()", equalTo(1))
                .body("[0].title", equalTo("Match"));
    }

    // ------------------------------------------------------------
    // PATCH /notes/{id}  (Update Success)
    // ------------------------------------------------------------
    @Test
    void testPatchNote_success() {
        NoteDTO patch = new NoteDTO("Updated Title", null);

        Note updated = new Note("5", "Updated Title", "Existing Content", System.currentTimeMillis());
        updated.setUpdatedAt(updated.getCreatedAt());

        when(service.updateNote("5", "Updated Title", null))
                .thenReturn(Uni.createFrom().item(updated));

        given()
                .contentType("application/json")
                .body(patch)
                .when()
                .patch("/notes/5")
                .then()
                .statusCode(200)
                .body("message", equalTo("Note updated!"))
                .body("data.title", equalTo("Updated Title"));
    }

    // ------------------------------------------------------------
    // PATCH /notes/{id}  (Not found)
    // ------------------------------------------------------------
    @Test
    void testPatchNote_notFound() {
        NoteDTO patch = new NoteDTO("x", "y");

        when(service.updateNote("20", "x", "y"))
                .thenReturn(Uni.createFrom().nullItem());

        given()
                .contentType("application/json")
                .body(patch)
                .when()
                .patch("/notes/20")
                .then()
                .statusCode(404)
                .body("message", equalTo("Update failed no note found"));
    }

    // ------------------------------------------------------------
    // PATCH /notes/{id}  (Bad Request - no fields)
    // ------------------------------------------------------------
    @Test
    void testPatchNote_emptyBody() {
        NoteDTO patch = new NoteDTO(null, null); // no fields

        given()
                .contentType("application/json")
                .body(patch)
                .when()
                .patch("/notes/1")
                .then()
                .statusCode(400)
                .body("message", equalTo("No fields provided to update"));
    }

    // ------------------------------------------------------------
    // DELETE /notes/{id}  (Success)
    // ------------------------------------------------------------
    @Test
    void testDeleteNote_success() {
        Note removed = new Note("8", "Old", "Data", System.currentTimeMillis());
        removed.setUpdatedAt(removed.getCreatedAt());

        when(service.deleteNote("8"))
                .thenReturn(Uni.createFrom().item(removed));

        when()
                .delete("/notes/8")
                .then()
                .statusCode(200)
                .body("message", equalTo("Note deleted successfully!"));
    }

    // ------------------------------------------------------------
    // DELETE /notes/{id}  (Not found)
    // ------------------------------------------------------------
    @Test
    void testDeleteNote_notFound() {
        when(service.deleteNote("404"))
                .thenReturn(Uni.createFrom().nullItem());

        when()
                .delete("/notes/404")
                .then()
                .statusCode(404)
                .body("message", equalTo("Delete failed no note found"));
    }
}