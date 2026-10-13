package com.dotani.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/** Comments and reactions on posts. */
class PostEngagementIntegrationTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Fixture f;

    @BeforeEach
    void setUp() {
        f = data.standardFixture();
    }

    @Test
    void memberReactsOnceAndCanChangeTheReaction() throws Exception {
        String postId = createPost(f.ward().getId(), "PUBLISHED");

        react(postId, "LIKE");
        react(postId, "LOVE");
        mockMvc.perform(auth.as(f.tdp2Member(), put("/api/posts/{id}/reaction", postId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("type", "LOVE"))))
                .andExpect(status().isOk());

        mockMvc.perform(auth.as(f.tdp1Member(), post("/api/posts/engagement"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("postIds", List.of(postId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalReactions").value(2))
                .andExpect(jsonPath("$[0].reactionCounts.LOVE").value(2))
                .andExpect(jsonPath("$[0].myReaction").value("LOVE"));

        mockMvc.perform(auth.as(f.tdp1Member(), delete("/api/posts/{id}/reaction", postId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReactions").value(1))
                .andExpect(jsonPath("$.myReaction").doesNotExist());
    }

    @Test
    void commentsNotifyTheAuthorAndOnlyTheAuthorEdits() throws Exception {
        String postId = createPost(f.ward().getId(), "PUBLISHED");

        var created = mockMvc.perform(auth.as(f.tdp1Member(), post("/api/posts/{id}/comments", postId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "  Rat hay!  "))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Rat hay!"))
                .andExpect(jsonPath("$.authorName").value("Doan vien TDP 1"))
                .andExpect(jsonPath("$.canEdit").value(true))
                .andReturn();
        String commentId = body(created).get("id").asText();

        assertThat(jdbcTemplate.queryForList(
                "select n.reference_id from notifications n join user_notifications un on un.notification_id = n.id"
                        + " where un.member_id = ? and n.notification_type = 'POST_COMMENT'",
                String.class, f.wardSecretary().getId())).containsExactly(postId);

        mockMvc.perform(auth.as(f.tdp2Member(), put("/api/posts/{id}/comments/{commentId}", postId, commentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "Sua"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMENT_FORBIDDEN"));
        mockMvc.perform(auth.as(f.tdp2Member(), delete("/api/posts/{id}/comments/{commentId}", postId, commentId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(auth.as(f.tdp2Member(), get("/api/posts/{id}/comments", postId)))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].canEdit").value(false))
                .andExpect(jsonPath("$.content[0].canDelete").value(false));

        // ward officers moderate
        mockMvc.perform(auth.as(f.wardDeputy(), delete("/api/posts/{id}/comments/{commentId}", postId, commentId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/posts/{id}/comments", postId)))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void draftsAndOutOfScopePostsTakeNoEngagement() throws Exception {
        String draftId = createPost(f.ward().getId(), "DRAFT");
        mockMvc.perform(auth.as(f.tdp1Member(), post("/api/posts/{id}/comments", draftId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "Hello"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(auth.as(f.wardSecretary(), put("/api/posts/{id}/reaction", draftId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("type", "LIKE"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POST_NOT_PUBLISHED"));

        String tdp1Post = createPost(f.tdp1().getId(), "PUBLISHED");
        mockMvc.perform(auth.as(f.tdp2Member(), put("/api/posts/{id}/reaction", tdp1Post))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("type", "LIKE"))))
                .andExpect(status().isForbidden());
        // the batch lookup silently leaves out what the caller cannot read
        mockMvc.perform(auth.as(f.tdp2Member(), post("/api/posts/engagement"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("postIds", List.of(tdp1Post, draftId)))))
                .andExpect(jsonPath("$.length()").value(0));
    }

    private void react(String postId, String type) throws Exception {
        mockMvc.perform(auth.as(f.tdp1Member(), put("/api/posts/{id}/reaction", postId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("type", type))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myReaction").value(type));
    }

    private String createPost(String organizationId, String status) throws Exception {
        var result = mockMvc.perform(auth.as(f.wardSecretary(), post("/api/posts"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Bai viet",
                                "content", "Noi dung",
                                "type", "NEWS",
                                "organizationId", organizationId,
                                "status", status))))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("id").asText();
    }
}
