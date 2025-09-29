package com.challenge.integracion.service;

import com.challenge.integracion.dto.MergedPostResponse;
import com.challenge.integracion.model.placeholder.Comment;
import com.challenge.integracion.model.placeholder.Post;
import com.challenge.integracion.model.placeholder.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private final RestTemplate restTemplate;
    private final ExternalPlaceholderClient client;
    private final String baseUrl;

    public PostService(RestTemplate restTemplate,
                       ExternalPlaceholderClient client,
                       @Value("${external.jsonplaceholder.base-url:https://jsonplaceholder.typicode.com}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.client = client;
        this.baseUrl = baseUrl;
    }

    public List<MergedPostResponse> getMergedPosts() {
        try {
            log.info("Llamando a API externa: posts, users y comments");
            Post[] posts = client.fetchPosts();
            User[] users = client.fetchUsers();
            Comment[] comments = client.fetchComments();

            Map<Integer, User> usersById = Arrays.stream(users)
                    .collect(Collectors.toMap(User::id, Function.identity()));

            Map<Integer, List<Comment>> commentsByPostId = Arrays.stream(comments)
                    .collect(Collectors.groupingBy(Comment::postId));

            return Arrays.stream(posts)
                    .map(p -> toMerged(p, usersById.get(p.userId()),
                            commentsByPostId.getOrDefault(p.id(), List.of())))
                    .collect(Collectors.toList());

        } catch (HttpStatusCodeException e) {
            log.error("Error HTTP al consumir JSONPlaceholder: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (RestClientException e) {
            log.error("Fallo de comunicación con JSONPlaceholder", e);
            throw e;
        }
    }

    public ResponseEntity<Void> deletePostById(int id) {
        try {
            String url = baseUrl + "/posts/" + id;
            log.info("Enviando DELETE a {}", url);
            return restTemplate.exchange(url, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        } catch (HttpStatusCodeException e) {
            log.warn("DELETE retorna error HTTP: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (RestClientException e) {
            log.error("Fallo de comunicacion en DELETE", e);
            throw e;
        }
    }

    private MergedPostResponse toMerged(Post p, User u, List<Comment> comments) {
        MergedPostResponse.UserSummary userSummary = (u == null)
                ? new MergedPostResponse.UserSummary(0, "desconocido", null, null)
                : new MergedPostResponse.UserSummary(u.id(), u.name(), u.username(), u.email());

        List<MergedPostResponse.CommentSummary> commentSummaries = comments.stream()
                .map(c -> new MergedPostResponse.CommentSummary(c.id(), c.name(), c.email(), c.body()))
                .collect(Collectors.toList());

        return new MergedPostResponse(p.id(), p.title(), p.body(), userSummary, commentSummaries);
    }
}
