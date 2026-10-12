package com.dotani.support;

import com.dotani.shared.security.AccessScope;
import com.dotani.shared.security.Role;
import com.dotani.auth.entity.UserAccount;
import com.dotani.auth.service.JwtService;
import com.dotani.member.entity.Member;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Issues real access tokens so tests exercise the production JWT filter and scope checks. */
@Component
public class TestAuth {

    private final JwtService jwtService;

    public TestAuth(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /** Token for a seeded member: role and scope are taken from the member's own record. */
    public String tokenFor(Member member) {
        String organizationId = member.getOrganization().getId();
        String wardId = member.getOrganization().getParent() == null
                ? organizationId
                : member.getOrganization().getParent().getId();
        String tdpId = member.getOrganization().getParent() == null ? null : organizationId;
        return token(
                member.getUserId(),
                member.getId(),
                Role.valueOf(member.getMemberRole().name()),
                wardId,
                tdpId
        );
    }

    public String token(String userId, String memberId, Role role, String organizationId, String tdpId) {
        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setUsername(userId);
        user.setEmail(userId + "@dotani.test");
        user.setMemberId(memberId);
        return jwtService.createAccessToken(user, new AccessScope(role, organizationId, tdpId));
    }

    public String expiredToken(Member member) {
        UserAccount user = new UserAccount();
        user.setId(member.getUserId());
        user.setUsername(member.getUserId());
        user.setEmail(member.getUserId() + "@dotani.test");
        user.setMemberId(member.getId());
        Instant past = Instant.now().minusSeconds(3600);
        AccessScope scope = new AccessScope(Role.valueOf(member.getMemberRole().name()), null, null);
        return jwtService.createAccessToken(user, scope, past.minusSeconds(60), past);
    }

    public MockHttpServletRequestBuilder as(Member member, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(member));
    }

    /** Identity given explicitly; the member does not need to exist (mirrors the old gateway headers). */
    public RequestPostProcessor identity(
            String userId,
            String role,
            String organizationId,
            String tdpId,
            String memberId
    ) {
        String token = token(userId, memberId, Role.valueOf(role), organizationId, tdpId);
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return request;
        };
    }
}
