package com.challenge.integracion.controller;

import com.challenge.integracion.dto.MergedPostResponse;
import com.challenge.integracion.exception.ApiExceptionHandler;
import com.challenge.integracion.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
@Import(ApiExceptionHandler.class)
class PostControllerTest {

    @Autowired
    MockMvc mvc;

    @org.springframework.boot.test.mock.mockito.MockBean
    PostService postService;

    @Test
    void getPosts_ok() throws Exception {
        var resp = List.of(new MergedPostResponse(
                10, "t", "b",
                new MergedPostResponse.UserSummary(1, "Leanne", "Bret", "l@example.com"),
                List.of(new MergedPostResponse.CommentSummary(100, "n", "e@e.com", "c"))
        ));
        given(postService.getMergedPosts()).willReturn(resp);

        mvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(10)))
                .andExpect(jsonPath("$[0].user.name", is("Leanne")))
                .andExpect(jsonPath("$[0].comments", hasSize(1)));
    }

    @Test
    void deletePost_ok() throws Exception {
        given(postService.deletePostById(anyInt())).willReturn(ResponseEntity.status(OK).build());

        mvc.perform(delete("/posts/1"))
                .andExpect(status().isOk());
    }

    @Test
    void deletePost_badRequest_whenIdInvalid() throws Exception {
        // id=0 viola @Min(1) en el controller
        mvc.perform(delete("/posts/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", containsString("Bad Request")));
    }
}
