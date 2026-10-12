package com.dotani.member.service;

import com.dotani.member.entity.Member;
import com.dotani.member.entity.OrganizationUnit;
import com.dotani.member.repository.MemberRepository;
import com.dotani.shared.exception.ApiException;
import com.dotani.shared.security.AccessScope;
import com.dotani.shared.security.Role;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Derives an account's role and scope from its member profile when tokens are issued. */
@Service
public class MemberAccessService {

    private final MemberRepository memberRepository;

    public MemberAccessService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public AccessScope scopeOf(String memberId) {
        Member member = memberId == null ? null : memberRepository.findById(memberId).orElse(null);
        if (member == null) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_LINKED",
                    "This account is not linked to a member profile"
            );
        }
        OrganizationUnit organization = member.getOrganization();
        boolean inTdp = organization.getParent() != null;
        String wardId = inTdp ? organization.getParent().getId() : organization.getId();
        // Ward officers act for the whole ward even if their profile sits in a TDP.
        String tdpId = inTdp && !member.getMemberRole().isWardOfficer() ? organization.getId() : null;
        return new AccessScope(Role.valueOf(member.getMemberRole().name()), wardId, tdpId);
    }
}
