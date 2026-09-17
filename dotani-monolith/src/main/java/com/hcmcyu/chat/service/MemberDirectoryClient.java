package com.hcmcyu.chat.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MemberDirectoryClient {

    private final com.hcmcyu.member.service.InternalMemberDirectoryService memberDirectoryService;
    private final String internalSecret;

    public MemberDirectoryClient(
            com.hcmcyu.member.service.InternalMemberDirectoryService memberDirectoryService,
            @Value("${services.member.internal-secret:dev-internal-secret}") String internalSecret
    ) {
        this.memberDirectoryService = memberDirectoryService;
        this.internalSecret = internalSecret;
    }

    public Map<String, String> findDisplayNames(Collection<String> memberIds) {
        List<String> ids = memberIds.stream()
                .filter(memberId -> memberId != null && !memberId.isBlank())
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        try {
            return memberDirectoryService.findDisplayNames(
                            new com.hcmcyu.member.dto.MemberDisplayNameRequest(ids),
                            internalSecret
                    )
                    .stream()
                    .collect(Collectors.toMap(
                            com.hcmcyu.member.dto.MemberDisplayNameResponse::memberId,
                            com.hcmcyu.member.dto.MemberDisplayNameResponse::fullName,
                            (left, right) -> left
                    ));
        } catch (RuntimeException exception) {
            return Map.of();
        }
    }
}
