package com.challenge.integracion.dto;

import java.util.List;


public record MergedPostResponse(
        int id,
        String title,
        String body,
        UserSummary user,
        List<CommentSummary> comments
) {
    public record UserSummary(int id, String name, String username, String email) {}
    public record CommentSummary(int id, String name, String email, String body) {}
}
