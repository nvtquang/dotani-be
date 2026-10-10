package com.dotani.member.service;

import com.dotani.member.entity.Member;
import com.dotani.member.repository.MemberRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Display names for other modules (chat senders, conversation members). */
@Service
public class MemberDirectoryService {

    private final MemberRepository memberRepository;

    public MemberDirectoryService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, String> findDisplayNames(Collection<String> memberIds) {
        Set<String> ids = memberIds.stream()
                .filter(memberId -> memberId != null && !memberId.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            return Map.of();
        }
        return memberRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Member::getId, Member::getFullName, (left, right) -> left));
    }
}
