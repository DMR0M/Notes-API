package com.example.notes.service;

import com.example.notes.entity.Note;
import com.example.notes.repository.NoteRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Random;
import java.util.UUID;

@ApplicationScoped
public class NoteService {
    @Inject
    NoteRepository repo;

    public Uni<Note> createNote(String title, String content) {
        Note note = new Note(
                UUID.randomUUID().toString(),
                title,
                content,
                System.currentTimeMillis()
        );

        return repo.create(note);
    }

    public Multi<Note> listNotes() {

        return repo.list();
    }

    public Uni<Note> getNote(String id) {
        return repo.findNoteById(id);
    }

    public Multi<Note> searchNotes(String title, String content) {
        return repo.findByTitleOrContent(title, content);
    }

    public Uni<Note> updateNote(String id, String title, String content) {
        return shouldIThrowAnError()
                ? Uni.createFrom().failure(new Exception("The error devil called you!"))
                : repo.update(id, title, content);
    }

    public Uni<Note> deleteNote(String id) {
        return repo.delete(id);
    }

    boolean shouldIThrowAnError() {
        Random random = new Random();
        int value = random.nextInt(10);

        return value > 5;
    }
}
