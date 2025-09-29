package com.challenge.integracion.controller;

import com.challenge.integracion.dto.MergedPostResponse;
import com.challenge.integracion.service.PostService;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/posts")
public class PostController {

    private static final Logger log = LoggerFactory.getLogger(PostController.class);
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public ResponseEntity<List<MergedPostResponse>> getPosts() {
        log.debug("GET /posts llamado");
        List<MergedPostResponse> data = postService.getMergedPosts();
        return ResponseEntity.ok(data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable @Min(1) int id) {
        log.debug("DELETE /posts/{} llamado", id);
        ResponseEntity<Void> external = postService.deletePostById(id);
        HttpStatusCode status = external.getStatusCode();
        return ResponseEntity.status(status).build();
    }
}
