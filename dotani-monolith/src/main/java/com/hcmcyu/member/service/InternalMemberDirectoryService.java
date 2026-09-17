package com.hcmcyu.member.service;

import com.hcmcyu.member.dto.MemberDisplayNameRequest;
import com.hcmcyu.member.dto.MemberDisplayNameResponse;
import com.hcmcyu.member.exception.MemberServiceException;
import com.hcmcyu.member.repository.MemberRepository;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalMemberDirectoryService {

    private final MemberRepository memberRepository;
    private final String internalSecret;

    public InternalMemberDirectoryService(
            MemberRepository memberRepository,
            @Value("${services.auth.internal-secret:dev-internal-secret}") String internalSecret
    ) {
        this.memberRepository = memberRepository;
        this.internalSecret = internalSecret;
    }

    @Transactional(readOnly = true)
    public List<MemberDisplayNameResponse> findDisplayNames(
            MemberDisplayNameRequest request,
            String providedSecret
    ) {
        if (providedSecret == null || !providedSecret.equals(internalSecret)) {
            throw new MemberServiceException(HttpStatus.UNAUTHORIZED, "INVALID_INTERNAL_SECRET", "Invalid internal secret");
        }

        LinkedHashSet<String> ids = new LinkedHashSet<>(request.memberIds().stream()
                .filter(memberId -> memberId != null && !memberId.isBlank())
                .map(String::trim)
                .toList());

        return memberRepository.findAllById(ids).stream()
                .map(member -> new MemberDisplayNameResponse(member.getId(), member.getFullName()))
                .toList();
    }
}
