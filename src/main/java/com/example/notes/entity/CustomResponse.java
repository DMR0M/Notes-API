package com.example.notes.entity;

public record CustomResponse<T> (String message, T data) { }
