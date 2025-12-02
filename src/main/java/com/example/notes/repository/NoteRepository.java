package com.example.notes.repository;

import com.example.notes.entity.Note;
import com.example.notes.utils.StorageUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class NoteRepository {
    private final Map<String, Note> storage = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final File file = new File(StorageUtils.STORAGE_PATH);

    /**
     * Initializes the in-memory note storage by loading existing data from the
     * JSON file defined in {@link StorageUtils#STORAGE_PATH}. This method is
     * executed once after the {@link NoteRepository} instance is created and all
     * dependencies are injected.
     *
     * <p>If the storage file exists, its contents are deserialized into the
     * internal {@code storage} map using Jackson's {@link ObjectMapper}. If the
     * file does not exist, the repository starts with an empty storage.
     *
     * <p>This method is triggered automatically by the {@link PostConstruct}
     * annotation and should not be called manually.
     */
    @PostConstruct
    void init() {
        if (file.exists()) {
            try {
                System.out.println(file.getAbsolutePath());
                Map<String, Note> loaded = objectMapper.readValue(file, new TypeReference<>() {} );
                storage.putAll(loaded);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void persist() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, storage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Uni<Note> create(Note note) {
        storage.put(note.id(), note);
        persist();
        return Uni.createFrom().item(note);
    }

    public Multi<Note> list() {
        return Multi.createFrom().items(storage.values().stream());
    }

    public Uni<Note> findNoteById(String id) {
        Note found = storage.get(id);

        return Uni.createFrom().item(found);
    }

    public Multi<Note> findByTitleOrContent(String title, String content) {
        return Multi.createFrom().iterable(storage.values())
                .filter(note -> {
                        boolean matchesTitle = title != null && note.title().toLowerCase()
                                .contains(title.toLowerCase());
                        boolean matchesContent = content != null && note.content().toLowerCase()
                                .contains(content.toLowerCase());

                        return matchesTitle || matchesContent;
                });
    }

    public Uni<Note> update(String id, String title, String content) {
        Note existingNote = storage.get(id);

        if (existingNote == null) {
            return Uni.createFrom().item(null);
        }

        Note updatedNote = existingNote.withTitleAndContent(
                title != null ? title : existingNote.title(),
                content != null ? content : existingNote.content()
        );

        storage.put(id, updatedNote);
        persist();

        return Uni.createFrom().item(updatedNote);
    }


    public Uni<Note> delete(String id) {
        Note removedNote = storage.remove(id);

        if (removedNote != null) {
            persist();
        }

        return Uni.createFrom().item(removedNote);
    }
}
