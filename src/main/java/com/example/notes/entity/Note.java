package com.example.notes.entity;

public record Note(
        String id,
        String title,
        String content,
        long createdAt,
        long updatedAt
) {
    // Factory method for creating a Note without setting the value for updatedAt field
    public static Note create(String id, String title, String content) {
        long timeNow = System.currentTimeMillis();
        return new Note(id, title, content, timeNow, timeNow);
    }

    public Note withTitleAndContent(String newTitle, String newContent) {
        long timeNow = System.currentTimeMillis();
        return new Note(id, newTitle, newContent, createdAt, timeNow);
    }
}
