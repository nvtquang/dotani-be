package com.hcmcyu.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcmcyu.chat.dto.MessageResponse;
import com.hcmcyu.chat.entity.Conversation;
import com.hcmcyu.chat.entity.ConversationMember;
import com.hcmcyu.chat.entity.ConversationType;
import com.hcmcyu.chat.repository.ConversationMemberRepository;
import com.hcmcyu.chat.repository.ConversationRepository;
import com.hcmcyu.chat.repository.MessageRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:hcmcyu_chat_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "auth.jwt.secret=test-hcmcyu-chat-service-secret-key-change-me-please-32"
})
class ChatIntegrationTest {

    private static final String JWT_SECRET = "test-hcmcyu-chat-service-secret-key-change-me-please-32";

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMemberRepository conversationMemberRepository;

    @Autowired
    private MessageRepository messageRepository;

    private Conversation directConversation;
    private Conversation groupConversation;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        conversationMemberRepository.deleteAll();
        conversationRepository.deleteAll();

        directConversation = saveConversation(ConversationType.DIRECT, null, "member-1", "member-2");
        groupConversation = saveConversation(ConversationType.GROUP, "TDP 1", "member-1", "member-2", "member-3");
    }

    @Test
    void memberCanCreateDirectAndGroupConversations() throws Exception {
        MvcResult directResult = mockMvc.perform(post("/api/chat/conversations")
                        .header(HttpHeaders.AUTHORIZATION, bearer("member-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "type", "DIRECT",
                                "memberIds", List.of("member-2")
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DIRECT"))
                .andExpect(jsonPath("$.memberIds[?(@ == 'member-1')]").exists())
                .andExpect(jsonPath("$.memberIds[?(@ == 'member-2')]").exists())
                .andReturn();

        String directId = objectMapper.readTree(directResult.getResponse().getContentAsString()).get("id").asText();
        assertThat(conversationMemberRepository.existsByConversation_IdAndMemberId(directId, "member-1")).isTrue();

        mockMvc.perform(post("/api/chat/conversations")
                        .header(HttpHeaders.AUTHORIZATION, bearer("member-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "type", "GROUP",
                                "title", "Nhom hoat dong",
                                "memberIds", List.of("member-2", "member-3")
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("GROUP"))
                .andExpect(jsonPath("$.title").value("Nhom hoat dong"))
                .andExpect(jsonPath("$.memberIds.length()").value(3));
    }

    @Test
    void onlyConversationMembersCanReadConversationAndHistory() throws Exception {
        mockMvc.perform(get("/api/chat/conversations/{id}", directConversation.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer("member-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(directConversation.getId()));

        mockMvc.perform(get("/api/chat/conversations/{id}", directConversation.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer("outsider")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CONVERSATION_ACCESS_DENIED"));

        mockMvc.perform(get("/api/chat/conversations/{id}/messages", directConversation.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer("outsider")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CONVERSATION_ACCESS_DENIED"));
    }

    @Test
    void groupMemberCanAddAndRemoveMembers() throws Exception {
        mockMvc.perform(post("/api/chat/conversations/{id}/members", groupConversation.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer("member-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("memberId", "member-4"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberIds[?(@ == 'member-4')]").exists());

        mockMvc.perform(delete("/api/chat/conversations/{id}/members/{memberId}", groupConversation.getId(), "member-4")
                        .header(HttpHeaders.AUTHORIZATION, bearer("member-1")))
                .andExpect(status().isNoContent());

        assertThat(conversationMemberRepository.existsByConversation_IdAndMemberId(groupConversation.getId(), "member-4"))
                .isFalse();
    }

    @Test
    void memberCanUploadChatAttachment() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bao-cao.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf-content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/chat/conversations/{id}/messages/attachments", groupConversation.getId())
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer("member-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderId").value("member-1"))
                .andExpect(jsonPath("$.attachmentKind").value("FILE"))
                .andExpect(jsonPath("$.attachmentName").value("bao-cao.pdf"))
                .andExpect(jsonPath("$.attachmentUrl").value(org.hamcrest.Matchers.startsWith("/uploads/chat/attachments/")));

        assertThat(messageRepository.findAll()).hasSize(1);
        assertThat(messageRepository.findAll().getFirst().getAttachmentUrl()).startsWith("/uploads/chat/attachments/");
    }

    @Test
    void groupMemberCanUpdateGroupAvatar() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "nhom.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[] {1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/chat/conversations/{id}/avatar", groupConversation.getId())
                        .file(file)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .header(HttpHeaders.AUTHORIZATION, bearer("member-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(org.hamcrest.Matchers.startsWith("/uploads/chat/group-avatars/")));
    }

    @Test
    void outsiderCannotManageGroupMembers() throws Exception {
        mockMvc.perform(post("/api/chat/conversations/{id}/members", groupConversation.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer("outsider"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("memberId", "member-4"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CONVERSATION_ACCESS_DENIED"));
    }

    @Test
    void websocketMessageUsesSenderFromJwtBroadcastsAndPersists() throws Exception {
        WebSocketStompClient stompClient = stompClient();

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add(HttpHeaders.AUTHORIZATION, bearer("member-1"));
        StompSession session = stompClient
                .connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {}
                )
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<MessageResponse> messages = new ArrayBlockingQueue<>(1);
        session.subscribe("/topic/conversations/" + directConversation.getId(), new MessageFrameHandler(messages));
        Thread.sleep(300);

        session.send("/app/chat/" + directConversation.getId(), Map.of(
                "senderId", "forged-sender",
                "content", "Xin chao realtime"
        ));

        MessageResponse message = messages.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message.senderId()).isEqualTo("member-1");
        assertThat(message.content()).isEqualTo("Xin chao realtime");
        assertThat(messageRepository.findAll()).hasSize(1);
        assertThat(messageRepository.findAll().getFirst().getSenderId()).isEqualTo("member-1");
    }

    @Test
    void outsiderCannotSubscribeOrSendMessagesToConversation() throws Exception {
        WebSocketStompClient stompClient = stompClient();

        StompSession outsiderSession = connect(stompClient, "outsider");
        BlockingQueue<MessageResponse> outsiderMessages = new ArrayBlockingQueue<>(1);
        outsiderSession.subscribe(
                "/topic/conversations/" + directConversation.getId(),
                new MessageFrameHandler(outsiderMessages)
        );
        Thread.sleep(300);
        assertThat(outsiderSession.isConnected()).isFalse();

        outsiderSession = connect(stompClient, "outsider");
        outsiderSession.send("/app/chat/" + directConversation.getId(), Map.of("content", "Blocked message"));
        Thread.sleep(300);
        assertThat(messageRepository.findAll()).isEmpty();

        StompSession memberSession = connect(stompClient, "member-1");
        memberSession.send("/app/chat/" + directConversation.getId(), Map.of("content", "Visible to members only"));

        assertThat(outsiderMessages.poll(1, TimeUnit.SECONDS)).isNull();
        assertThat(messageRepository.findAll()).hasSize(1);
        assertThat(messageRepository.findAll().getFirst().getSenderId()).isEqualTo("member-1");
    }

    @Test
    void invalidMessageContentIsRejectedAndNotPersisted() throws Exception {
        WebSocketStompClient stompClient = stompClient();

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add(HttpHeaders.AUTHORIZATION, bearer("member-1"));
        StompSession session = stompClient
                .connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {}
                )
                .get(5, TimeUnit.SECONDS);

        session.send("/app/chat/" + directConversation.getId(), Map.of("content", "   "));
        Thread.sleep(300);

        assertThat(messageRepository.findAll()).isEmpty();
    }

    private Conversation saveConversation(ConversationType type, String title, String... memberIds) {
        Conversation conversation = new Conversation();
        conversation.setType(type);
        conversation.setTitle(title);
        conversation.setCreatedBy(memberIds[0]);
        for (String memberId : memberIds) {
            ConversationMember member = new ConversationMember();
            member.setMemberId(memberId);
            conversation.addMember(member);
        }
        return conversationRepository.saveAndFlush(conversation);
    }

    private String bearer(String memberId) {
        return "Bearer " + token(memberId);
    }

    private String token(String memberId) {
        SecretKey secretKey = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(memberId + "-user")
                .claim("userId", memberId + "-user")
                .claim("memberId", memberId)
                .claim("username", memberId)
                .claim("email", memberId + "@example.com")
                .claim("role", "MEMBER")
                .claim("organizationId", "ward-thuong-cat")
                .claim("tdpId", "tdp-1")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .signWith(secretKey)
                .compact();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private StompSession connect(WebSocketStompClient stompClient, String memberId) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add(HttpHeaders.AUTHORIZATION, bearer(memberId));
        return stompClient
                .connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {}
                )
                .get(5, TimeUnit.SECONDS);
    }

    private WebSocketStompClient stompClient() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.initialize();
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(converter);
        stompClient.setTaskScheduler(taskScheduler);
        return stompClient;
    }

    private static class MessageFrameHandler implements StompFrameHandler {

        private final BlockingQueue<MessageResponse> messages;

        MessageFrameHandler(BlockingQueue<MessageResponse> messages) {
            this.messages = messages;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return MessageResponse.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            messages.add((MessageResponse) payload);
        }
    }
}
