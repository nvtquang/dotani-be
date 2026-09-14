package com.hcmcyu.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    private static final String JWT_SECRET = "test-hcmcyu-gateway-secret-key-change-me-please-32";
    private static final String WRONG_JWT_SECRET = "wrong-hcmcyu-gateway-secret-key-change-me-please-32";

    private static final MockWebServer AUTH_SERVICE = startMockServer();
    private static final MockWebServer MEMBER_SERVICE = startMockServer();
    private static final MockWebServer EVENT_SERVICE = startMockServer();
    private static final MockWebServer CONTENT_SERVICE = startMockServer();
    private static final MockWebServer CHAT_SERVICE = startMockServer();
    private static final MockWebServer NOTIFICATION_SERVICE = startMockServer();
    private static final MockWebServer AUDIT_SERVICE = startMockServer();

    @LocalServerPort
    private int port;

    @Autowired
    private RouteLocator routeLocator;

    @DynamicPropertySource
    static void registerServiceUrls(DynamicPropertyRegistry registry) {
        registry.add("AUTH_SERVICE_URL", () -> AUTH_SERVICE.url("/").toString());
        registry.add("MEMBER_SERVICE_URL", () -> MEMBER_SERVICE.url("/").toString());
        registry.add("EVENT_SERVICE_URL", () -> EVENT_SERVICE.url("/").toString());
        registry.add("CONTENT_SERVICE_URL", () -> CONTENT_SERVICE.url("/").toString());
        registry.add("CHAT_SERVICE_URL", () -> CHAT_SERVICE.url("/").toString());
        registry.add("CHAT_SERVICE_WS_URL", () -> "ws://localhost:" + CHAT_SERVICE.getPort());
        registry.add("NOTIFICATION_SERVICE_URL", () -> NOTIFICATION_SERVICE.url("/").toString());
        registry.add("AUDIT_SERVICE_URL", () -> AUDIT_SERVICE.url("/").toString());
        registry.add("AUTH_JWT_SECRET", () -> JWT_SECRET);
    }

    @AfterAll
    static void stopMockServers() throws IOException {
        AUTH_SERVICE.shutdown();
        MEMBER_SERVICE.shutdown();
        EVENT_SERVICE.shutdown();
        CONTENT_SERVICE.shutdown();
        CHAT_SERVICE.shutdown();
        NOTIFICATION_SERVICE.shutdown();
        AUDIT_SERVICE.shutdown();
    }

    @Test
    void routesHealthRequestsToServices() throws InterruptedException {
        expectHealth(AUTH_SERVICE, "auth-service");
        expectHealth(MEMBER_SERVICE, "member-service");
        expectHealth(MEMBER_SERVICE, "member-service");
        expectHealth(EVENT_SERVICE, "event-service");
        expectHealth(CONTENT_SERVICE, "content-service");
        expectHealth(CHAT_SERVICE, "chat-service");
        expectHealth(NOTIFICATION_SERVICE, "notification-service");

        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        assertHealth(client, "/api/auth/health", "auth-service");
        assertHealth(client, "/api/members/health", "member-service");
        assertHealth(client, "/api/organizations/health", "member-service");
        assertHealth(client, "/api/events/health", "event-service");
        assertHealth(client, "/api/posts/health", "content-service");
        assertHealth(client, "/api/chat/health", "chat-service");
        assertHealth(client, "/api/notifications/health", "notification-service");

        assertThat(AUTH_SERVICE.takeRequest().getPath()).isEqualTo("/api/health");
        assertThat(MEMBER_SERVICE.takeRequest().getPath()).isEqualTo("/api/health");
        assertThat(MEMBER_SERVICE.takeRequest().getPath()).isEqualTo("/api/health");
        assertThat(EVENT_SERVICE.takeRequest().getPath()).isEqualTo("/api/health");
        assertThat(CONTENT_SERVICE.takeRequest().getPath()).isEqualTo("/api/health");
        assertThat(CHAT_SERVICE.takeRequest().getPath()).isEqualTo("/api/health");
        assertThat(NOTIFICATION_SERVICE.takeRequest().getPath()).isEqualTo("/api/health");
    }

    @Test
    void exposesChatWebSocketRoute() {
        assertThat(routeLocator.getRoutes().map(Route::getId).collectList().block())
                .contains("chat-service-websocket");
    }

    @Test
    void routesAuditLogRequestsToAuditService() throws InterruptedException {
        AUDIT_SERVICE.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"content\":[],\"totalElements\":0}"));

        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        client.get()
                .uri("/api/audit-logs?page=0&size=10")
                .header("Authorization", bearer("ward-secretary-user", null, "WARD_SECRETARY", "ward-thuong-cat", null))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(0);

        RecordedRequest request = AUDIT_SERVICE.takeRequest();
        assertThat(request.getPath()).isEqualTo("/api/audit-logs?page=0&size=10");
        assertThat(request.getHeader("X-User-Id")).isEqualTo("ward-secretary-user");
        assertThat(request.getHeader("X-User-Role")).isEqualTo("WARD_SECRETARY");
    }

    @Test
    void routesPublicOrganizationListWithoutToken() throws InterruptedException {
        MEMBER_SERVICE.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[{\"id\":\"tdp-1\",\"name\":\"Chi doan TDP 1\",\"type\":\"YOUTH_UNION_BRANCH\"}]"));

        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        client.get()
                .uri("/api/organizations/public")
                .header("X-User-Id", "forged-user")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("tdp-1");

        RecordedRequest request = MEMBER_SERVICE.takeRequest();
        assertThat(request.getPath()).isEqualTo("/api/organizations/public");
        assertThat(request.getHeader("X-User-Id")).isNull();
    }

    @Test
    void routesUploadedMemberAssetsWithoutToken() throws InterruptedException {
        MEMBER_SERVICE.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "image/png")
                .setBody("avatar"));

        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        client.get()
                .uri("/uploads/avatars/demo.png")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("avatar");

        client.get()
                .uri("/uploads/bank-qr/demo.png")
                .exchange()
                .expectStatus().isNotFound();

        assertThat(MEMBER_SERVICE.takeRequest().getPath()).isEqualTo("/uploads/avatars/demo.png");
    }

    @Test
    void gatewayRejectsProtectedRequestsWithoutToken() {
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        client.get()
                .uri("/api/audit-logs")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void gatewayRejectsInvalidTokenSignature() {
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        client.get()
                .uri("/api/members")
                .header("Authorization", bearerWithSecret(
                        WRONG_JWT_SECRET,
                        "member-a-user",
                        "member-a",
                        "MEMBER",
                        "ward-thuong-cat",
                        "tdp-1"
                ))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_TOKEN");
    }

    @Test
    void gatewayStripsSpoofedIdentityHeadersAndInjectsVerifiedJwtClaims() throws InterruptedException {
        MEMBER_SERVICE.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"content\":[],\"totalElements\":0}"));

        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        client.get()
                .uri("/api/members")
                .header("Authorization", bearer("member-a-user", "member-a", "MEMBER", "ward-thuong-cat", "tdp-1"))
                .header("X-User-Id", "forged-ward-secretary")
                .header("X-User-Role", "WARD_SECRETARY")
                .header("X-Tdp-Id", "tdp-2")
                .exchange()
                .expectStatus().isOk();

        RecordedRequest request = MEMBER_SERVICE.takeRequest();
        assertThat(request.getHeader("X-User-Id")).isEqualTo("member-a-user");
        assertThat(request.getHeader("X-Member-Id")).isEqualTo("member-a");
        assertThat(request.getHeader("X-User-Role")).isEqualTo("MEMBER");
        assertThat(request.getHeader("X-Tdp-Id")).isEqualTo("tdp-1");
    }

    private static String bearer(
            String userId,
            String memberId,
            String role,
            String organizationId,
            String tdpId
    ) {
        return bearerWithSecret(JWT_SECRET, userId, memberId, role, organizationId, tdpId);
    }

    private static String bearerWithSecret(
            String secret,
            String userId,
            String memberId,
            String role,
            String organizationId,
            String tdpId
    ) {
        SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return "Bearer " + Jwts.builder()
                .subject(userId)
                .claim("userId", userId)
                .claim("memberId", memberId)
                .claim("username", userId)
                .claim("email", userId + "@example.com")
                .claim("role", role)
                .claim("organizationId", organizationId)
                .claim("tdpId", tdpId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .signWith(secretKey)
                .compact();
    }

    private static MockWebServer startMockServer() {
        MockWebServer server = new MockWebServer();
        try {
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start mock server", exception);
        }
    }

    private static void expectHealth(MockWebServer server, String service) {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"service\":\"" + service + "\",\"status\":\"UP\"}"));
    }

    private static void assertHealth(WebTestClient client, String path, String service) {
        client.get()
                .uri(path)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.service").isEqualTo(service)
                .jsonPath("$.status").isEqualTo("UP");
    }
}
