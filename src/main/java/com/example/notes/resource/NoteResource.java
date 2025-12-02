package com.example.notes.resource;

import com.example.notes.entity.CustomResponse;
import com.example.notes.exception.NoteNotFoundException;
import com.example.notes.request.NoteDTO;
import com.example.notes.service.NoteService;
import com.example.notes.entity.Note;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


/**
 * REST resource for managing notes with reactive endpoints using Mutiny.
 * All endpoints produce and consume JSON and delegate business logic to {@link NoteService}.
 *
 * <p>Available endpoints:
 * <ul>
 *     <li>POST /notes - Create a new note</li>
 *     <li>GET /notes - List all notes</li>
 *     <li>GET /notes/{id} - Retrieve a note by ID</li>
 *     <li>GET /notes/search - Search notes by title and/or content</li>
 *     <li>PATCH /notes/{id} - Partially update a note</li>
 *     <li>DELETE /notes/{id} - Delete a note by ID</li>
 * </ul>
 *
 * <p>Reactive return types:
 * {@link Uni}&lt;Response&gt; for single-result endpoints, {@link Multi}&lt;Note&gt; for multi-result streams.
 */
@Path("/notes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NoteResource {
    @Inject
    NoteService service;

    /**
     * Creates a new note.
     *
     * @param note the Note object containing title and content to be created
     * @return a Uni emitting a Response with status 201 (Created) and the created note as entity
     */
    @POST
    public Uni<Response> create(Note note) {
        return service.createNote(note.getTitle(), note.getContent())
                .onItem()
                .transform(createNote -> Response.status(Response.Status.CREATED)
                        .entity(new CustomResponse<>("Note created successfully!", createNote))
                        .build()
                );
    }

    /**
     * Retrieves all notes.
     *
     * @return a {@link Uni} emitting a response containing a {@link CustomResponse}:
     *      - 200 OK with the list of all notes in {@code data} if retrieval is successful
     *
     * Note: The list may be empty if no notes exist
     */
    @GET
    public Uni<Response> getAllNotes() {
        return service.listNotes()
                .collect()
                .asList()
                .onItem().transform(notes -> {
                    String message = notes.isEmpty() ? "No notes found" : "All notes retrieved!";
                    return Response.ok(new CustomResponse<>(message, notes)).build();
                });
    }

    /**
     * Retrieves a note by its unique ID.
     *
     * @param id the ID of the note to retrieve
     * @return a Uni emitting a Response:
     *         - 200 OK with the note if found
     *         - 404 Not Found if no note exists with the given ID
     */
    @GET
    @Path("/{id}")
    public Uni<Response> getNoteById(@PathParam("id") String id) {
        return service.getNote(id)
                .onItem()
                .transform(note -> Response.ok(
                        new CustomResponse<>("Note found!", note)).build()
                )
                .onFailure(NoteNotFoundException.class)
                .recoverWithItem((ex) -> Response.status(Response.Status.NOT_FOUND)
                        .entity(new CustomResponse<>(ex.getMessage(), null))
                        .build()
                );
    }

    /**
     * Searches notes by title and/or content.
     *
     * This endpoint allows filtering notes using optional query parameters.
     * At least one of the query parameters must be provided.
     * The search is case-insensitive and matches notes where the title or content contains the given values.
     *
     * @param title   optional query parameter to filter notes by title
     * @param content optional query parameter to filter notes by content
     * @return a Uni emitting a Response:
     *          - 200 OK with a list of matching notes in {@code data} if search is successful
     *          - 400 Bad Request with {@code data = null} if both query parameters are missing or empty
     */
    @GET
    @Path("/search")
    public Uni<Response> searchNotes(@QueryParam("title") String title, @QueryParam("content") String content) {
        // Handle no passed fields from the request body
        if ((title == null || title.isBlank()) && (content == null || content.isBlank())) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new CustomResponse<>("At least one search parameter must be provided", null))
                    .build());
        }

        return service.searchNotes(title, content)
                .collect()
                .asList()
                .onItem()
                .transform(notes -> {
                    String message = notes.isEmpty() ? "No notes found that matches the title or content" : "Notes retrieved!";
                    return Response.ok(new CustomResponse<>(message, notes)).build();
                });
    }

    /**
     * Updates a note partially (PATCH operation).
     * Only non-null fields in the NoteDTO are updated.
     *
     * @param id   the ID of the note to update
     * @param note a NoteDTO containing fields to update; at least one field must be non-null
     * @return a Uni emitting a Response:
     *         - 200 OK with updated note if update succeeds
     *         - 400 Bad Request if no fields are provided
     *         - 404 Not Found if no note exists with the given ID
     */
    @PATCH
    @Path("/{id}")
    public Uni<Response> patchNote(@PathParam("id") String id, NoteDTO note) {
        // Handle no passed fields from the request body
        if (note.getTitle() == null && note.getContent() == null) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new CustomResponse<>("No fields provided to update", null))
                    .build()
            );
        }

        return service.updateNote(id, note.getTitle(), note.getContent())
                .onItem()
                .transform(updatedNote -> Response.ok(
                        new CustomResponse<>("Note updated!", updatedNote)).build()
                )
                .onFailure(NoteNotFoundException.class)
                .recoverWithItem((ex) -> Response.status(Response.Status.NOT_FOUND)
                        .entity(new CustomResponse<>(ex.getMessage(), null))
                        .build()
                );
    }

    /**
     * Deletes a note by its unique ID.
     *
     * @param id the ID of the note to delete
     * @return a Uni emitting a Response:
     *         - 200 OK with deleted note if deletion succeeds
     *         - 404 Not Found if no note exists with the given ID
     */
    @DELETE
    @Path("/{id}")
    public Uni<Response> deleteNote(@PathParam("id") String id) {
        return service.deleteNote(id)
                .onItem()
                .transform(deletedNote -> Response.ok(
                        new CustomResponse<>("Note deleted successfully!", deletedNote)).build()
                )
                .onFailure(NoteNotFoundException.class)
                .recoverWithItem((ex) -> Response.status(Response.Status.NOT_FOUND)
                        .entity(new CustomResponse<>(ex.getMessage(), null))
                        .build()
                );
    }
}
