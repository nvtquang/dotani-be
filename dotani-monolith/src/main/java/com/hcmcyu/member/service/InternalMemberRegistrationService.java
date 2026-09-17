package com.hcmcyu.member.service;

import com.hcmcyu.member.dto.InternalMemberRegistrationRequest;
import com.hcmcyu.member.dto.InternalMemberProfileCompletionRequest;
import com.hcmcyu.member.dto.MemberResponse;
import com.hcmcyu.member.entity.Member;
import com.hcmcyu.member.entity.MemberRole;
import com.hcmcyu.member.entity.MemberStatus;
import com.hcmcyu.member.entity.OrganizationUnit;
import com.hcmcyu.member.entity.OrganizationUnitType;
import com.hcmcyu.member.exception.MemberServiceException;
import com.hcmcyu.member.mapper.MemberMapper;
import com.hcmcyu.member.repository.MemberRepository;
import com.hcmcyu.member.repository.OrganizationUnitRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalMemberRegistrationService {

    private final MemberRepository memberRepository;
    private final OrganizationUnitRepository organizationUnitRepository;
    private final MemberMapper memberMapper;
    private final String internalSecret;

    public InternalMemberRegistrationService(
            MemberRepository memberRepository,
            OrganizationUnitRepository organizationUnitRepository,
            MemberMapper memberMapper,
            @Value("${services.auth.internal-secret:dev-internal-secret}") String internalSecret
    ) {
        this.memberRepository = memberRepository;
        this.organizationUnitRepository = organizationUnitRepository;
        this.memberMapper = memberMapper;
        this.internalSecret = internalSecret;
    }

    @Transactional
    public MemberResponse registerMemberProfile(
            InternalMemberRegistrationRequest request,
            String providedSecret
    ) {
        if (providedSecret == null || !providedSecret.equals(internalSecret)) {
            throw new MemberServiceException(HttpStatus.UNAUTHORIZED, "INVALID_INTERNAL_SECRET", "Invalid internal secret");
        }
        if (memberRepository.existsByUserId(request.userId())) {
            throw new MemberServiceException(HttpStatus.CONFLICT, "DUPLICATE_MEMBER_USER_ID", "Member userId already exists");
        }

        OrganizationUnit organization = organizationUnitRepository.findById(request.organizationId().trim())
                .orElseThrow(() -> new MemberServiceException(
                        HttpStatus.NOT_FOUND,
                        "ORGANIZATION_NOT_FOUND",
                        "Organization unit not found"
                ));
        if (organization.getType() != OrganizationUnitType.YOUTH_UNION_BRANCH || !organization.isActive()) {
            throw new MemberServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_MEMBER_ORGANIZATION",
                    "Member must belong to an active youth union branch"
            );
        }

        Member member = new Member();
        member.setUserId(request.userId().trim());
        member.setFullName(request.fullName().trim());
        member.setEmail(request.email() == null ? null : request.email().trim().toLowerCase());
        member.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        member.setDateOfBirth(request.dateOfBirth());
        member.setOrganization(organization);
        member.setMemberRole(MemberRole.MEMBER);
        member.setMemberStatus(MemberStatus.ACTIVE);

        return memberMapper.toResponse(memberRepository.save(member));
    }

    @Transactional
    public MemberResponse completeMemberProfile(
            String memberId,
            InternalMemberProfileCompletionRequest request,
            String providedSecret
    ) {
        if (providedSecret == null || !providedSecret.equals(internalSecret)) {
            throw new MemberServiceException(HttpStatus.UNAUTHORIZED, "INVALID_INTERNAL_SECRET", "Invalid internal secret");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberServiceException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Member not found"));

        member.setFullName(request.fullName().trim());
        member.setPhone(request.phone().trim());
        member.setDateOfBirth(request.dateOfBirth());

        return memberMapper.toResponse(memberRepository.save(member));
    }
}
