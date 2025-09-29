package com.challenge.integracion.service;

import com.challenge.integracion.model.placeholder.Comment;
import com.challenge.integracion.model.placeholder.Post;
import com.challenge.integracion.model.placeholder.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
public class ExternalPlaceholderClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalPlaceholderClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ExternalPlaceholderClient(RestTemplate restTemplate,
                                     @Value("${external.jsonplaceholder.base-url:https://jsonplaceholder.typicode.com}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public Post[] fetchPosts() {
        log.debug("HTTP -> GET {}/posts", baseUrl);
        return Optional.ofNullable(restTemplate.getForObject(baseUrl + "/posts", Post[].class))
                .orElse(new Post[0]);
    }

    @Cacheable("users")
    public User[] fetchUsers() {
        log.debug("HTTP -> GET {}/users (si no hay HIT de caché)", baseUrl);
        return Optional.ofNullable(restTemplate.getForObject(baseUrl + "/users", User[].class))
                .orElse(new User[0]);
    }

    @Cacheable("comments")
    public Comment[] fetchComments() {
        log.debug("HTTP -> GET {}/comments (si no hay HIT de caché)", baseUrl);
        return Optional.ofNullable(restTemplate.getForObject(baseUrl + "/comments", Comment[].class))
                .orElse(new Comment[0]);
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
