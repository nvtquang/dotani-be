package com.dotani.member.service;

import com.dotani.member.entity.Member;
import com.dotani.member.entity.MemberRole;
import com.dotani.member.entity.MemberStatus;
import com.dotani.member.entity.OrganizationUnit;
import com.dotani.member.entity.OrganizationUnitType;
import com.dotani.member.repository.MemberRepository;
import com.dotani.member.repository.OrganizationUnitRepository;
import com.dotani.shared.exception.ApiException;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates and completes the member profile behind a self-registered (local or Google) account. */
@Service
public class MemberRegistrationService {

    private final MemberRepository memberRepository;
    private final OrganizationUnitRepository organizationUnitRepository;

    public MemberRegistrationService(
            MemberRepository memberRepository,
            OrganizationUnitRepository organizationUnitRepository
    ) {
        this.memberRepository = memberRepository;
        this.organizationUnitRepository = organizationUnitRepository;
    }

    /**
     * @param tdpId   the TDP (youth union branch) the member joins
     * @param wardId  the ward that TDP belongs to; accounts carry it as their organization scope
     */
    public record RegisteredMember(String memberId, String tdpId, String wardId) {
    }

    @Transactional
    public RegisteredMember register(
            String userId,
            String fullName,
            String phone,
            LocalDate dateOfBirth,
            String organizationId
    ) {
        if (memberRepository.existsAnyByUserId(userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_MEMBER_USER_ID", "Member userId already exists");
        }

        OrganizationUnit organization = organizationUnitRepository.findById(organizationId.trim())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ORGANIZATION_NOT_FOUND",
                        "Organization unit not found"
                ));
        if (organization.getType() != OrganizationUnitType.YOUTH_UNION_BRANCH || !organization.isActive()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_MEMBER_ORGANIZATION",
                    "Member must belong to an active youth union branch"
            );
        }

        Member member = new Member();
        member.setUserId(userId);
        member.setFullName(fullName.trim());
        // the email lives on the login account (user_accounts.email)
        member.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        member.setDateOfBirth(dateOfBirth);
        member.setOrganization(organization);
        member.setMemberRole(MemberRole.MEMBER);
        member.setMemberStatus(MemberStatus.ACTIVE);
        Member saved = memberRepository.save(member);

        String wardId = organization.getParent() == null ? organization.getId() : organization.getParent().getId();
        return new RegisteredMember(saved.getId(), organization.getId(), wardId);
    }

    @Transactional
    public void completeProfile(String memberId, String fullName, String phone, LocalDate dateOfBirth) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Member not found"));
        member.setFullName(fullName.trim());
        member.setPhone(phone.trim());
        member.setDateOfBirth(dateOfBirth);
    }
}
