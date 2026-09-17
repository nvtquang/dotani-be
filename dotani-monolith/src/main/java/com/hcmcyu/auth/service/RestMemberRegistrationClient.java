package com.hcmcyu.auth.service;

import com.hcmcyu.auth.dto.GoogleLoginRequest;
import com.hcmcyu.auth.dto.MemberRegistrationResponse;
import com.hcmcyu.auth.dto.RegisterRequest;
import com.hcmcyu.auth.entity.UserAccount;
import com.hcmcyu.member.dto.InternalMemberProfileCompletionRequest;
import com.hcmcyu.member.dto.InternalMemberRegistrationRequest;
import com.hcmcyu.member.dto.MemberResponse;
import com.hcmcyu.member.service.InternalMemberRegistrationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RestMemberRegistrationClient implements MemberRegistrationClient {

    private final InternalMemberRegistrationService memberRegistrationService;
    private final String internalSecret;

    public RestMemberRegistrationClient(
            InternalMemberRegistrationService memberRegistrationService,
            @Value("${auth.internal.secret:dev-internal-secret}") String internalSecret
    ) {
        this.memberRegistrationService = memberRegistrationService;
        this.internalSecret = internalSecret;
    }

    @Override
    public MemberRegistrationResponse createMemberProfile(UserAccount user, RegisterRequest request) {
        return createMemberProfile(
                user,
                request.fullName(),
                request.phone(),
                request.dateOfBirth(),
                request.organizationId()
        );
    }

    @Override
    public MemberRegistrationResponse createMemberProfile(UserAccount user, GoogleLoginRequest request) {
        return createMemberProfile(
                user,
                request.fullName(),
                request.phone(),
                request.dateOfBirth(),
                request.organizationId()
        );
    }

    @Override
    public MemberRegistrationResponse completeMemberProfile(UserAccount user, GoogleLoginRequest request) {
        MemberResponse member = memberRegistrationService.completeMemberProfile(
                user.getMemberId(),
                new InternalMemberProfileCompletionRequest(
                        request.fullName().trim(),
                        request.phone().trim(),
                        request.dateOfBirth()
                ),
                internalSecret
        );
        return toRegistrationResponse(member);
    }

    private MemberRegistrationResponse createMemberProfile(
            UserAccount user,
            String fullName,
            String phone,
            java.time.LocalDate dateOfBirth,
            String organizationId
    ) {
        MemberResponse member = memberRegistrationService.registerMemberProfile(
                new InternalMemberRegistrationRequest(
                        user.getId(),
                        fullName.trim(),
                        user.getEmail(),
                        blankToNull(phone),
                        dateOfBirth,
                        organizationId.trim()
                ),
                internalSecret
        );
        return toRegistrationResponse(member);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private MemberRegistrationResponse toRegistrationResponse(MemberResponse member) {
        return new MemberRegistrationResponse(
                member.id(),
                member.userId(),
                member.fullName(),
                member.email(),
                member.organizationId(),
                member.organizationName()
        );
    }
}
