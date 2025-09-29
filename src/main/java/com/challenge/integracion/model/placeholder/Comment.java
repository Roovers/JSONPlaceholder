package com.challenge.integracion.model.placeholder;

public record Comment(
        int postId,
        int id,
        String name,
        String email,
        String body
) {}
