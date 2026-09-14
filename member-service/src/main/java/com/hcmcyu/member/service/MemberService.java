package com.hcmcyu.member.service;

import com.hcmcyu.member.dto.MemberBankingRequest;
import com.hcmcyu.member.dto.MemberBankingResponse;
import com.hcmcyu.member.dto.MemberDirectoryResponse;
import com.hcmcyu.member.dto.MemberProfileUpdateRequest;
import com.hcmcyu.member.dto.MemberRequest;
import com.hcmcyu.member.dto.MemberResponse;
import com.hcmcyu.member.dto.MemberRoleUpdateRequest;
import com.hcmcyu.member.entity.Member;
import com.hcmcyu.member.entity.MemberRole;
import com.hcmcyu.member.entity.MemberRoleChangeAudit;
import com.hcmcyu.member.entity.MemberStatus;
import com.hcmcyu.member.entity.OrganizationUnit;
import com.hcmcyu.member.entity.OrganizationUnitType;
import com.hcmcyu.member.exception.MemberServiceException;
import com.hcmcyu.member.mapper.MemberMapper;
import com.hcmcyu.member.repository.MemberRepository;
import com.hcmcyu.member.repository.MemberRoleChangeAuditRepository;
import com.hcmcyu.member.repository.MemberSpecifications;
import com.hcmcyu.member.repository.OrganizationUnitRepository;
import com.hcmcyu.member.security.CurrentUser;
import com.hcmcyu.member.security.Role;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final OrganizationUnitRepository organizationUnitRepository;
    private final MemberRoleChangeAuditRepository memberRoleChangeAuditRepository;
    private final MemberMapper memberMapper;
    private final MemberScopeService memberScopeService;
    private final AuthRoleClient authRoleClient;
    private final AuditClient auditClient;
    private final StorageService storageService;

    public MemberService(
            MemberRepository memberRepository,
            OrganizationUnitRepository organizationUnitRepository,
            MemberRoleChangeAuditRepository memberRoleChangeAuditRepository,
            MemberMapper memberMapper,
            MemberScopeService memberScopeService,
            AuthRoleClient authRoleClient,
            AuditClient auditClient,
            StorageService storageService
    ) {
        this.memberRepository = memberRepository;
        this.organizationUnitRepository = organizationUnitRepository;
        this.memberRoleChangeAuditRepository = memberRoleChangeAuditRepository;
        this.memberMapper = memberMapper;
        this.memberScopeService = memberScopeService;
        this.authRoleClient = authRoleClient;
        this.auditClient = auditClient;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public Page<MemberResponse> findAll(
            String keyword,
            String organizationId,
            MemberStatus status,
            MemberRole role,
            Pageable pageable,
            CurrentUser currentUser
    ) {
        Specification<Member> specification = Specification
                .where(MemberSpecifications.withinScope(currentUser))
                .and(MemberSpecifications.keywordContains(keyword))
                .and(MemberSpecifications.hasOrganization(organizationId))
                .and(MemberSpecifications.hasStatus(status))
                .and(MemberSpecifications.hasRole(role));

        return memberRepository.findAll(specification, pageable)
                .map(memberMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<MemberDirectoryResponse> findDirectory(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().length() < 2) {
            return Page.empty(pageable);
        }

        Specification<Member> specification = Specification
                .where(MemberSpecifications.keywordContains(keyword))
                .and(MemberSpecifications.hasStatus(MemberStatus.ACTIVE));

        return memberRepository.findAll(specification, pageable)
                .map(memberMapper::toDirectoryResponse);
    }

    @Transactional(readOnly = true)
    public MemberResponse findById(String id, CurrentUser currentUser) {
        Member member = getMember(id);
        memberScopeService.requireRead(currentUser, member);
        return memberMapper.toResponse(member);
    }

    @Transactional(readOnly = true)
    public MemberResponse findCurrentProfile(CurrentUser currentUser) {
        Member member = getCurrentMember(currentUser);
        return memberMapper.toResponse(member);
    }

    @Transactional(readOnly = true)
    public MemberBankingResponse findCurrentBanking(CurrentUser currentUser) {
        Member member = getCurrentMember(currentUser);
        return toBankingResponse(member);
    }

    @Transactional(readOnly = true)
    public MemberBankingResponse findBankingByMemberId(String memberId, CurrentUser currentUser) {
        Member member = getMember(memberId);
        memberScopeService.requireRead(currentUser, member);
        return toBankingResponse(member);
    }

    @Transactional(readOnly = true)
    public Resource loadBankQrImage(String memberId, CurrentUser currentUser) {
        Member member = getMember(memberId);
        memberScopeService.requireRead(currentUser, member);
        return storageService.loadBankQr(member.getBankQrImageUrl());
    }

    @Transactional
    public MemberResponse create(MemberRequest request, CurrentUser currentUser) {
        validateUniqueUserIdForCreate(request.userId());

        OrganizationUnit organization = getOrganization(request.organizationId());
        validateMemberOrganization(organization);
        memberScopeService.requireCreateInOrganization(currentUser, organization.getId());

        Member member = new Member();
        applyRequest(member, request, organization);
        member.setMemberRole(MemberRole.MEMBER);

        Member saved = memberRepository.save(member);
        auditClient.record(AuditAction.CREATE_MEMBER, saved, currentUser);
        return memberMapper.toResponse(saved);
    }

    @Transactional
    public MemberResponse update(String id, MemberRequest request, CurrentUser currentUser) {
        Member member = getMember(id);
        memberScopeService.requireWrite(currentUser, member);
        validateUniqueUserIdForUpdate(request.userId(), id);
        String oldOrganizationId = member.getOrganization().getId();

        OrganizationUnit organization = getOrganization(request.organizationId());
        validateMemberOrganization(organization);
        memberScopeService.requireCreateInOrganization(currentUser, organization.getId());

        applyRequest(member, request, organization);
        memberScopeService.requireWrite(currentUser, member);
        auditClient.record(AuditAction.UPDATE_MEMBER, member, currentUser);
        if (!oldOrganizationId.equals(organization.getId())) {
            auditClient.record(AuditAction.CHANGE_ORGANIZATION, member, currentUser);
        }
        return memberMapper.toResponse(member);
    }

    @Transactional
    public MemberResponse updateCurrentProfile(MemberProfileUpdateRequest request, CurrentUser currentUser) {
        Member member = getCurrentMember(currentUser);
        member.setFullName(request.fullName().trim());
        member.setDateOfBirth(request.dateOfBirth());
        member.setGender(request.gender());
        member.setPhone(blankToNull(request.phone()));
        member.setEmail(request.email() == null ? null : request.email().trim().toLowerCase());
        member.setAddress(blankToNull(request.address()));
        return memberMapper.toResponse(member);
    }

    @Transactional
    public MemberBankingResponse updateCurrentBanking(MemberBankingRequest request, CurrentUser currentUser) {
        Member member = getCurrentMember(currentUser);
        member.setBankName(blankToNull(request.bankName()));
        member.setBankCode(blankToNull(request.bankCode()));
        member.setAccountNumber(blankToNull(request.accountNumber()));
        member.setAccountHolderName(blankToNull(request.accountHolderName()));
        return toBankingResponse(member);
    }

    @Transactional
    public void softDelete(String id, CurrentUser currentUser) {
        Member member = getMember(id);
        memberScopeService.requireWrite(currentUser, member);
        member.setMemberStatus(MemberStatus.INACTIVE);
        auditClient.record(AuditAction.DISABLE_MEMBER, member, currentUser);
    }

    @Transactional
    public MemberResponse updateRole(String memberId, MemberRoleUpdateRequest request, CurrentUser currentUser) {
        requireWardSecretary(currentUser);

        Member member = getMember(memberId);
        MemberRole oldRole = member.getMemberRole();
        MemberRole newRole = request.role();

        validateRoleTransition(member, oldRole, newRole);

        if (oldRole != newRole) {
            member.setMemberRole(newRole);
            auditRoleChange(member, oldRole, newRole, currentUser);
            auditClient.record(AuditAction.CHANGE_ROLE, member, currentUser);
            if (member.getUserId() != null) {
                authRoleClient.updateUserRole(member.getUserId(), newRole, currentUser.userId(), member.getId());
            }
        }

        return memberMapper.toResponse(member);
    }

    @Transactional
    public MemberResponse uploadCurrentAvatar(MultipartFile file, CurrentUser currentUser) {
        Member member = getCurrentMember(currentUser);
        String oldAvatarUrl = member.getAvatarUrl();
        String avatarUrl = storageService.storeAvatar(file);
        member.setAvatarUrl(avatarUrl);
        storageService.delete(oldAvatarUrl);
        return memberMapper.toResponse(member);
    }

    @Transactional
    public void deleteCurrentAvatar(CurrentUser currentUser) {
        Member member = getCurrentMember(currentUser);
        String oldAvatarUrl = member.getAvatarUrl();
        member.setAvatarUrl(null);
        storageService.delete(oldAvatarUrl);
    }

    @Transactional
    public MemberBankingResponse uploadCurrentBankQr(MultipartFile file, CurrentUser currentUser) {
        Member member = getCurrentMember(currentUser);
        String oldQrUrl = member.getBankQrImageUrl();
        String qrUrl = storageService.storeBankQr(file);
        member.setBankQrImageUrl(qrUrl);
        storageService.delete(oldQrUrl);
        return toBankingResponse(member);
    }

    @Transactional
    public void deleteCurrentBankQr(CurrentUser currentUser) {
        Member member = getCurrentMember(currentUser);
        String oldQrUrl = member.getBankQrImageUrl();
        member.setBankQrImageUrl(null);
        storageService.delete(oldQrUrl);
    }

    private void applyRequest(Member member, MemberRequest request, OrganizationUnit organization) {
        member.setUserId(blankToNull(request.userId()));
        member.setFullName(request.fullName().trim());
        member.setDateOfBirth(request.dateOfBirth());
        member.setGender(request.gender());
        member.setPhone(blankToNull(request.phone()));
        member.setEmail(request.email() == null ? null : request.email().trim().toLowerCase());
        member.setAddress(blankToNull(request.address()));
        member.setAvatarUrl(blankToNull(request.avatarUrl()));
        member.setYouthUnionJoinDate(request.youthUnionJoinDate());
        member.setMemberStatus(request.memberStatus() == null ? MemberStatus.ACTIVE : request.memberStatus());
        member.setOrganization(organization);
    }

    private OrganizationUnit getOrganization(String organizationId) {
        return organizationUnitRepository.findById(organizationId)
                .orElseThrow(() -> new MemberServiceException(
                        HttpStatus.NOT_FOUND,
                        "ORGANIZATION_NOT_FOUND",
                        "Organization unit not found"
                ));
    }

    private Member getMember(String id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new MemberServiceException(
                        HttpStatus.NOT_FOUND,
                        "MEMBER_NOT_FOUND",
                        "Member not found"
                ));
    }

    private Member getCurrentMember(CurrentUser currentUser) {
        if (currentUser.memberId() == null || currentUser.memberId().isBlank()) {
            throw new MemberServiceException(
                    HttpStatus.FORBIDDEN,
                    "MEMBER_CONTEXT_REQUIRED",
                    "Current user is not linked to a member profile"
            );
        }
        Member member = getMember(currentUser.memberId());
        memberScopeService.requireRead(currentUser, member);
        return member;
    }

    private void validateMemberOrganization(OrganizationUnit organization) {
        if (organization.getType() != OrganizationUnitType.YOUTH_UNION_BRANCH) {
            throw new MemberServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_MEMBER_ORGANIZATION",
                    "Member must belong to a youth union branch"
            );
        }
    }

    private void requireWardSecretary(CurrentUser currentUser) {
        if (currentUser.role() != Role.WARD_SECRETARY) {
            throw new MemberServiceException(
                    HttpStatus.FORBIDDEN,
                    "ROLE_UPDATE_FORBIDDEN",
                    "Only ward secretary can update member roles"
            );
        }
    }

    private void validateRoleTransition(Member member, MemberRole oldRole, MemberRole newRole) {
        if (requiresTdpOrganization(newRole) && member.getOrganization().getType() != OrganizationUnitType.YOUTH_UNION_BRANCH) {
            throw new MemberServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ROLE_ORGANIZATION",
                    "TDP roles require a youth union branch organization"
            );
        }

        if (oldRole == MemberRole.WARD_SECRETARY
                && newRole != MemberRole.WARD_SECRETARY
                && memberRepository.countByMemberRole(MemberRole.WARD_SECRETARY) <= 1) {
            throw new MemberServiceException(
                    HttpStatus.CONFLICT,
                    "LAST_WARD_SECRETARY",
                    "Cannot remove the last ward secretary role"
            );
        }
    }

    private boolean requiresTdpOrganization(MemberRole role) {
        return role == MemberRole.TDP_SECRETARY || role == MemberRole.TDP_DEPUTY_SECRETARY;
    }

    private void auditRoleChange(
            Member member,
            MemberRole oldRole,
            MemberRole newRole,
            CurrentUser currentUser
    ) {
        MemberRoleChangeAudit audit = new MemberRoleChangeAudit();
        audit.setMemberId(member.getId());
        audit.setUserId(member.getUserId());
        audit.setOldRole(oldRole);
        audit.setNewRole(newRole);
        audit.setActorUserId(currentUser.userId());
        audit.setActorMemberId(currentUser.memberId());
        memberRoleChangeAuditRepository.save(audit);
    }

    private MemberBankingResponse toBankingResponse(Member member) {
        return new MemberBankingResponse(
                member.getId(),
                member.getFullName(),
                member.getBankName(),
                member.getBankCode(),
                member.getAccountNumber(),
                member.getAccountHolderName(),
                member.getBankQrImageUrl() == null ? null : "/api/members/" + member.getId() + "/banking/qr-image"
        );
    }

    private void validateUniqueUserIdForCreate(String userId) {
        String normalizedUserId = blankToNull(userId);
        if (normalizedUserId != null && memberRepository.existsByUserId(normalizedUserId)) {
            throw duplicateUserId();
        }
    }

    private void validateUniqueUserIdForUpdate(String userId, String memberId) {
        String normalizedUserId = blankToNull(userId);
        if (normalizedUserId != null && memberRepository.existsByUserIdAndIdNot(normalizedUserId, memberId)) {
            throw duplicateUserId();
        }
    }

    private MemberServiceException duplicateUserId() {
        return new MemberServiceException(
                HttpStatus.CONFLICT,
                "DUPLICATE_MEMBER_USER_ID",
                "Member userId already exists"
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
