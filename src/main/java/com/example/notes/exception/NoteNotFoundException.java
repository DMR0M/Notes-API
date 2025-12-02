package com.example.notes.exception;

public class NoteNotFoundException extends RuntimeException {

    private final String noteId;

    public NoteNotFoundException(String noteId) {
        super(String.format("Note with id of %s not found", noteId));
        this.noteId = noteId;
    }

    public String getNoteId() {
        return noteId;
    }
}
