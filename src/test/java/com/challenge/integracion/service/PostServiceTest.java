package com.challenge.integracion.service;

import com.challenge.integracion.config.RestTemplateConfig;
import com.challenge.integracion.dto.MergedPostResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest
@Import({PostService.class, ExternalPlaceholderClient.class, RestTemplateConfig.class})
@TestPropertySource(properties = {
        "external.jsonplaceholder.base-url=https://jsonplaceholder.typicode.com",
        "http.connectTimeout=3000",
        "http.readTimeout=8000"
})
class PostServiceTest {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    PostService postService;

    MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.bindTo(restTemplate).build();

        server.expect(requestTo("https://jsonplaceholder.typicode.com/posts"))
                .andRespond(withSuccess(
                        """
                        [ {"userId":1,"id":10,"title":"t","body":"b"} ]
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://jsonplaceholder.typicode.com/users"))
                .andRespond(withSuccess(
                        """
                        [ {"id":1,"name":"Leanne","username":"Bret","email":"l@example.com"} ]
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://jsonplaceholder.typicode.com/comments"))
                .andRespond(withSuccess(
                        """
                        [ {"postId":10,"id":100,"name":"n","email":"e@e.com","body":"c"} ]
                        """, MediaType.APPLICATION_JSON));
    }

    @Test
    void getMergedPosts_ok() {
        List<MergedPostResponse> out = postService.getMergedPosts();

        assertThat(out).hasSize(1);
        var p = out.get(0);
        assertThat(p.id()).isEqualTo(10);
        assertThat(p.title()).isEqualTo("t");
        assertThat(p.user().name()).isEqualTo("Leanne");
        assertThat(p.comments()).hasSize(1);
        assertThat(p.comments().get(0).email()).isEqualTo("e@e.com");

        server.verify();
    }
}
