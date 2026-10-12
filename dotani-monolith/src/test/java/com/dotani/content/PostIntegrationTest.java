package com.dotani.content;

import com.dotani.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dotani.content.entity.Post;
import com.dotani.content.entity.PostStatus;
import com.dotani.content.entity.PostType;
import com.dotani.shared.attachment.Attachment;
import com.dotani.shared.attachment.AttachmentRepository;
import com.dotani.content.repository.PostRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class PostIntegrationTest extends IntegrationTest {

    private static final String WARD_ID = "ward-thuong-cat";
    private static final String TDP_1_ID = "tdp-1";
    private static final String TDP_2_ID = "tdp-2";
    private static final Path POST_IMAGE_STORAGE_PATH = Path.of("target/test-storage/post-images");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    private Post wardPost;
    private Post tdp1Post;
    private Post tdp2Post;
    private Post draftPost;

    @BeforeEach
    void setUp() throws Exception {
        data.legacyIds();
        cleanStorage();
        postRepository.deleteAll();

        wardPost = savePost("Thong bao phuong", PostType.ANNOUNCEMENT, WARD_ID, PostStatus.PUBLISHED, "ward-secretary");
        tdp1Post = savePost("Bao cao TDP 1", PostType.ACTIVITY_REPORT, TDP_1_ID, PostStatus.PUBLISHED, "tdp1-secretary");
        tdp2Post = savePost("Bao cao TDP 2", PostType.ACTIVITY_REPORT, TDP_2_ID, PostStatus.PUBLISHED, "tdp2-secretary");
        draftPost = savePost("Ban nhap TDP 1", PostType.ACTIVITY_REPORT, TDP_1_ID, PostStatus.DRAFT, "tdp1-secretary");
    }

    @Test
    void wardSecretaryCanCrudPostsAcrossWard() throws Exception {
        MvcResult result = mockMvc.perform(withWardSecretary(post("/api/posts"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(postPayload("Tin phuong", PostType.NEWS, TDP_2_ID, PostStatus.PUBLISHED))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Tin phuong"))
                .andExpect(jsonPath("$.organizationId").value(TDP_2_ID))
                .andReturn();

        String postId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(withWardSecretary(put("/api/posts/{id}", postId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(postPayload("Tin phuong updated", PostType.OTHER, TDP_1_ID, PostStatus.ARCHIVED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tin phuong updated"))
                .andExpect(jsonPath("$.organizationId").value(TDP_1_ID));

        mockMvc.perform(withWardSecretary(delete("/api/posts/{id}", postId)))
                .andExpect(status().isNoContent());

        assertThat(postRepository.findById(postId)).isEmpty();
    }

    @Test
    void tdpSecretaryCannotManageActivityReportInOwnTdp() throws Exception {
        mockMvc.perform(withTdpSecretary(post("/api/posts"), TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(postPayload("Bao cao moi TDP 1", PostType.ACTIVITY_REPORT, TDP_1_ID, PostStatus.PUBLISHED))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withTdpSecretary(put("/api/posts/{id}", tdp1Post.getId()), TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(postPayload("Bao cao moi TDP 1 updated", PostType.ACTIVITY_REPORT, TDP_1_ID, PostStatus.PUBLISHED))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withTdpSecretary(delete("/api/posts/{id}", tdp1Post.getId()), TDP_1_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));
    }

    @Test
    void tdpSecretaryCannotManageAnotherTdpOrNonReportPost() throws Exception {
        mockMvc.perform(withTdpSecretary(post("/api/posts"), TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(postPayload("Tin sai quyen", PostType.NEWS, TDP_1_ID, PostStatus.PUBLISHED))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withTdpSecretary(post("/api/posts"), TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(postPayload("Sai TDP", PostType.ACTIVITY_REPORT, TDP_2_ID, PostStatus.PUBLISHED))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withTdpSecretary(put("/api/posts/{id}", tdp2Post.getId()), TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(postPayload("Blocked", PostType.ACTIVITY_REPORT, TDP_2_ID, PostStatus.PUBLISHED))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));
    }

    @Test
    void memberCanOnlyViewPublishedPostsInScope() throws Exception {
        mockMvc.perform(withMember(get("/api/posts"), "member-1", TDP_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(wardPost.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(tdp1Post.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(tdp2Post.getId())).doesNotExist())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(draftPost.getId())).doesNotExist());

        mockMvc.perform(withMember(get("/api/posts/{id}", draftPost.getId()), "member-1", TDP_1_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withMember(post("/api/posts"), "member-1", TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(postPayload("Member blocked", PostType.ACTIVITY_REPORT, TDP_1_ID, PostStatus.PUBLISHED))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));
    }

    @Test
    void supportsOrganizationTypeDatePaginationFilters() throws Exception {
        mockMvc.perform(withWardSecretary(get("/api/posts")
                        .param("organization", TDP_1_ID)
                        .param("type", "ACTIVITY_REPORT")
                        .param("date", LocalDate.now().toString())
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,asc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(tdp1Post.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(draftPost.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(tdp2Post.getId())).doesNotExist());
    }

    @Test
    void officerCanUploadMultipleImagesAndDeleteImage() throws Exception {
        MockMultipartFile image1 = new MockMultipartFile("files", "report-1.png", "image/png", new byte[] {1, 2});
        MockMultipartFile image2 = new MockMultipartFile("files", "report-2.webp", "image/webp", new byte[] {3, 4});

        mockMvc.perform(withWardSecretary(multipart("/api/posts/{id}/images", tdp1Post.getId())
                        .file(image1)
                        .file(image2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.images[0].id").isNotEmpty())
                .andExpect(jsonPath("$.images[0].imageUrl").value(org.hamcrest.Matchers.startsWith("/uploads/post-images/")));

        var images = attachmentRepository.findAll();
        assertThat(images).hasSize(2);
        Attachment firstImage = images.getFirst();
        String firstUrl = firstImage.getUrl();
        String storedFilename = firstUrl.substring("/uploads/post-images/".length());
        assertThat(storedFilename).doesNotContain("report-1.png");
        assertThat(Files.exists(POST_IMAGE_STORAGE_PATH.resolve(storedFilename))).isTrue();

        String imageId = firstImage.getId();
        mockMvc.perform(withWardSecretary(delete("/api/posts/{id}/images/{imageId}", tdp1Post.getId(), imageId)))
                .andExpect(status().isNoContent());

        assertThat(attachmentRepository.findById(imageId)).isEmpty();
        assertThat(Files.exists(POST_IMAGE_STORAGE_PATH.resolve(storedFilename))).isFalse();
    }

    @Test
    void uploadImageValidatesFileTypeAndSize() throws Exception {
        // Posts accept images and office/pdf/txt documents; executables are still rejected.
        MockMultipartFile wrongType = new MockMultipartFile("files", "setup.exe", "application/x-msdownload", new byte[] {1});

        mockMvc.perform(withWardSecretary(multipart("/api/posts/{id}/images", tdp1Post.getId())
                        .file(wrongType)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_POST_ATTACHMENT_FILE"));

        MockMultipartFile tooLarge = new MockMultipartFile("files", "large.png", "image/png", new byte[17]);

        mockMvc.perform(withWardSecretary(multipart("/api/posts/{id}/images", tdp1Post.getId())
                        .file(tooLarge)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("POST_ATTACHMENT_TOO_LARGE"));
    }

    private Post savePost(
            String title,
            PostType type,
            String organizationId,
            PostStatus status,
            String authorId
    ) {
        Post post = new Post();
        post.setTitle(title);
        post.setContent(title + " content");
        post.setType(type);
        post.setOrganizationId(organizationId);
        post.setAuthorId(authorId);
        post.setStatus(status);
        return postRepository.save(post);
    }

    private Map<String, Object> postPayload(
            String title,
            PostType type,
            String organizationId,
            PostStatus status
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("content", title + " content");
        payload.put("type", type.name());
        payload.put("organizationId", organizationId);
        payload.put("status", status.name());
        return payload;
    }

    private MockHttpServletRequestBuilder withWardSecretary(MockHttpServletRequestBuilder request) {
        return request
                .with(auth.identity("ward-secretary-user", "WARD_SECRETARY", WARD_ID, null, "ward-secretary-member"));
    }

    private MockHttpServletRequestBuilder withTdpSecretary(MockHttpServletRequestBuilder request, String tdpId) {
        return request
                .with(auth.identity("tdp-secretary-user", "TDP_SECRETARY", WARD_ID, tdpId, "tdp-secretary-member"));
    }

    private MockHttpServletRequestBuilder withMember(
            MockHttpServletRequestBuilder request,
            String memberId,
            String tdpId
    ) {
        return request
                .with(auth.identity(memberId + "-user", "MEMBER", WARD_ID, tdpId, memberId));
    }


    private void cleanStorage() throws Exception {
        if (!Files.exists(POST_IMAGE_STORAGE_PATH)) {
            return;
        }
        try (var files = Files.walk(POST_IMAGE_STORAGE_PATH)) {
            files.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
        }
    }
}
