package com.example.notes.resource;

import com.example.notes.entity.CustomResponse;
import com.example.notes.exception.NoteNotFoundException;
import com.example.notes.request.NoteRequest;
import com.example.notes.service.NoteService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;


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
     * @param noteRequest the Note object containing title and content to be created
     * @return a Uni emitting a Response:
     *  - 201 (Created) and the created note as entity
     *  - 400 (Bad Request) if note request has a null or blank title
     */
    @POST
    @Operation(
            summary = "Create a new note",
            description = "Creates a new note with a title and optional content. Returns 201 on success or 400 if the title is missing."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "201",
                    description = "Note created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "successfulCreation",
                                    description = "Example response for a successfully created note",
                                    value = """
                {
                  "message": "Note created successfully!",
                  "data": {
                    "id": "f9b1c87d-8238-4a50-9f62-9b1c6d41e21d",
                    "title": "My First Note",
                    "content": "This is an example note."
                  }
                }
                """
                            )
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request — missing required fields",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "missingTitle",
                                    description = "Title field missing or blank",
                                    value = """
                {
                  "message": "Title is a required field",
                  "data": null
                }
                """
                            )
                    )
            )
    })
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = NoteRequest.class),
                    examples = @ExampleObject(
                            name = "noteRequestExample",
                            description = "Example payload for creating a note",
                            value = """
            {
              "title": "My First Note",
              "content": "This is some note content."
            }
            """
                    )
            )
    )
    public Uni<Response> createNote(NoteRequest noteRequest) {
        // Handle no passed fields from the request body
        if (noteRequest.title() == null || noteRequest.title().isBlank()) {
            return Uni.createFrom()
                    .item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new CustomResponse<>("Title is a required field", null))
                    .build()
            );
        }

        return service.createNote(noteRequest.title(), noteRequest.content())
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
    @Operation(
            summary = "Get all the existing notes",
            description = "Get all the notes saved from the JSON file " +
                    "located at the root project directory as the data storage"
    )
    @APIResponse(
            responseCode = "200",
            description = "Message value will be \"All notes retrieved!\" or \"No notes found when there are not saved notes\"",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CustomResponse.class)
            )
    )
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
    @Operation(
            summary = "Get a note by its unique ID",
            description = "Retrieve a single note using its unique identifier. Returns 404 if the note does not exist."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Note found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Note not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class)
                    )
            )
    })
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
    @Operation(
            summary = "Search notes by title and/or content",
            description = """
        Searches notes using optional `title` and `content` query parameters.
        At least one parameter must be provided. The search is case-insensitive
        and matches partial text in either title or content.
        """
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Notes matching the search criteria were found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "notesFound",
                                            description = "Notes that match the search criteria",
                                            value = """
                    {
                      "message": "Notes retrieved!",
                      "data": [
                        {
                          "id": "8c275792-b499-4f3e-9bd9-e8e1cfdce1db",
                          "title": "Meeting Notes",
                          "content": "Notes about the team meeting today."
                        },
                        {
                          "id": "7c275792-b499-4f3e-9bd9-e8e1cfdce123",
                          "title": "Grocery List",
                          "content": "Milk, Bread, Eggs"
                        }
                      ]
                    }
                    """
                                    ),
                                    @ExampleObject(
                                            name = "noResults",
                                            description = "No notes match the given search parameters",
                                            value = """
                    {
                      "message": "No notes found that matches the title or content",
                      "data": []
                    }
                    """
                                    )
                            }
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request — missing required query parameters",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "missingParams",
                                    description = "Neither title nor content was provided",
                                    value = """
                {
                  "message": "At least one search parameter must be provided",
                  "data": null
                }
                """
                            )
                    )
            )
    })
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
     * @param noteRequest a NoteDTO containing fields to update; at least one field must be non-null
     * @return a Uni emitting a Response:
     *         - 200 OK with updated note if update succeeds
     *         - 400 Bad Request if no fields are provided
     *         - 404 Not Found if no note exists with the given ID
     */
    @PATCH
    @Path("/{id}")
    @Operation(
            summary = "Partially update a note",
            description = """
        Partially updates a note by ID. Only non-null fields in the request body are updated.
        At least one of `title` or `content` must be provided. Returns the updated note as a response. 
        Returns 404 if the note does not exist.
        """
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Note updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "successfulUpdate",
                                    description = "Example response when a note was successfully updated",
                                    value = """
                {
                  "message": "Note updated!",
                  "data": {
                    "id": "abcd-1234",
                    "title": "Updated Title",
                    "content": "Updated content for the note."
                  }
                }
                """
                            )
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request — no fields provided to update",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "noFieldsProvided",
                                    description = "The user provided an empty request body or all fields were null",
                                    value = """
                {
                  "message": "No fields provided to update",
                  "data": null
                }
                """
                            )
                    )
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Note not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "noteNotFound",
                                    description = "Example response when attempting to update a non-existent note",
                                    value = """
                {
                  "message": "Note with ID 123 was not found",
                  "data": null
                }
                """
                            )
                    )
            )
    })
    @RequestBody(
            required = true,
            description = "Fields to update. At least one of `title` or `content` must be provided.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = NoteRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "updateTitleOnly",
                                    description = "Only updating the title",
                                    value = """
                {
                  "title": "Updated title"
                }
                """
                            ),
                            @ExampleObject(
                                    name = "updateContentOnly",
                                    description = "Only updating the content",
                                    value = """
                {
                  "content": "Updated content text"
                }
                """
                            ),
                            @ExampleObject(
                                    name = "updateBoth",
                                    description = "Updating both title and content",
                                    value = """
                {
                  "title": "Revised Title",
                  "content": "Revised content here"
                }
                """
                            )
                    }
            )
    )
    public Uni<Response> patchNote(@PathParam("id") String id, NoteRequest noteRequest) {
        // Handle no passed fields from the request body
        if (noteRequest.title() == null && noteRequest.content() == null) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new CustomResponse<>("No fields provided to update", null))
                    .build()
            );
        }

        return service.updateNote(id, noteRequest.title(), noteRequest.content())
                .onItem()
                .transform(updatedNote -> Response.ok(
                        new CustomResponse<>("Note updated!", updatedNote))
                        .build()
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
    @Operation(
            summary = "Delete a note by its unique id",
            description = "Delete the note by specifying its unique id and retrieve it. Returns 404 if the note does not exist."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The deleted note resource is sent as the response if delete request is successful",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Note not found ",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class)
                    )
            )
    })
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
