package com.dotani.member.service;

import com.dotani.audit.entity.AuditAction;
import com.dotani.audit.entity.ResourceType;
import com.dotani.audit.service.AuditDiff;
import com.dotani.audit.service.AuditLogService;
import com.dotani.auth.service.RefreshTokenService;
import com.dotani.member.dto.MemberTransferRequest;
import com.dotani.member.dto.MemberTransferResponse;
import com.dotani.member.entity.Member;
import com.dotani.member.entity.MemberRole;
import com.dotani.member.entity.MemberRoleChangeAudit;
import com.dotani.member.entity.MemberTransfer;
import com.dotani.member.entity.OrganizationUnit;
import com.dotani.member.entity.OrganizationUnitType;
import com.dotani.member.entity.TransferStatus;
import com.dotani.member.repository.MemberRepository;
import com.dotani.member.repository.MemberRoleChangeAuditRepository;
import com.dotani.member.repository.MemberTransferRepository;
import com.dotani.member.repository.OrganizationUnitRepository;
import com.dotani.shared.events.DomainEvents.MemberTransferDecided;
import com.dotani.shared.events.DomainEvents.MemberTransferRequested;
import com.dotani.shared.exception.ApiException;
import com.dotani.shared.security.CurrentUser;
import com.dotani.shared.security.OrgScopePolicy;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chuyển sinh hoạt. A member asks to move to another TDP (or an officer proposes it for a member they
 * manage); a ward officer or an officer of the receiving TDP approves or rejects it. Approving moves the
 * member; a TDP officer role does not travel with them and falls back to MEMBER.
 */
@Service
public class MemberTransferService {

    private final MemberTransferRepository transferRepository;
    private final MemberRepository memberRepository;
    private final OrganizationUnitRepository organizationUnitRepository;
    private final MemberRoleChangeAuditRepository roleChangeAuditRepository;
    private final MemberScopeService memberScopeService;
    private final MemberRoster memberRoster;
    private final NameLookup nameLookup;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher events;

    public MemberTransferService(
            MemberTransferRepository transferRepository,
            MemberRepository memberRepository,
            OrganizationUnitRepository organizationUnitRepository,
            MemberRoleChangeAuditRepository roleChangeAuditRepository,
            MemberScopeService memberScopeService,
            MemberRoster memberRoster,
            NameLookup nameLookup,
            RefreshTokenService refreshTokenService,
            AuditLogService auditLogService,
            ApplicationEventPublisher events
    ) {
        this.transferRepository = transferRepository;
        this.memberRepository = memberRepository;
        this.organizationUnitRepository = organizationUnitRepository;
        this.roleChangeAuditRepository = roleChangeAuditRepository;
        this.memberScopeService = memberScopeService;
        this.memberRoster = memberRoster;
        this.nameLookup = nameLookup;
        this.refreshTokenService = refreshTokenService;
        this.auditLogService = auditLogService;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Page<MemberTransferResponse> findAll(TransferStatus status, String memberId, Pageable pageable, CurrentUser currentUser) {
        Specification<MemberTransfer> specification = Specification
                .where(MemberTransferRepository.visibleTo(currentUser))
                .and(MemberTransferRepository.hasStatus(status))
                .and(MemberTransferRepository.ofMember(memberId));
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "requestedAt", "id"));
        Page<MemberTransfer> page = transferRepository.findAll(specification, sorted);
        Responses responses = responses(page.getContent());
        return page.map(transfer -> responses.of(transfer, currentUser));
    }

    @Transactional(readOnly = true)
    public MemberTransferResponse findById(String id, CurrentUser currentUser) {
        MemberTransfer transfer = getVisibleTransfer(id, currentUser);
        return responses(List.of(transfer)).of(transfer, currentUser);
    }

    @Transactional
    public MemberTransferResponse request(MemberTransferRequest request, CurrentUser currentUser) {
        String memberId = request.memberId() == null || request.memberId().isBlank() ? currentUser.memberId() : request.memberId().trim();
        if (memberId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "MEMBER_CONTEXT_REQUIRED", "Current user is not linked to a member profile");
        }
        Member member = getMember(memberId);
        if (!memberId.equals(currentUser.memberId())) {
            memberScopeService.requireWrite(currentUser, member);
        }
        OrganizationUnit target = organizationUnitRepository.findById(request.toOrganizationId().trim())
                .filter(unit -> unit.getType() == OrganizationUnitType.YOUTH_UNION_BRANCH && unit.isActive())
                .filter(unit -> !unit.getId().equals(member.getOrganization().getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TRANSFER_TARGET", "Choose another youth union branch"));
        if (transferRepository.existsByMemberIdAndStatus(memberId, TransferStatus.PENDING)) {
            throw new ApiException(HttpStatus.CONFLICT, "TRANSFER_ALREADY_PENDING", "Member already has a pending transfer");
        }

        MemberTransfer transfer = new MemberTransfer();
        transfer.setMemberId(memberId);
        transfer.setFromOrganizationId(member.getOrganization().getId());
        transfer.setToOrganizationId(target.getId());
        transfer.setReason(blankToNull(request.reason()));
        transfer.setRequestedBy(currentUser.memberId());
        MemberTransfer saved = transferRepository.save(transfer);
        auditLogService.record(currentUser, AuditAction.REQUEST_TRANSFER, ResourceType.TRANSFER, saved.getId(),
                saved.getFromOrganizationId(), AuditDiff.snapshot("memberId", memberId,
                        "fromOrganizationId", saved.getFromOrganizationId(), "toOrganizationId", saved.getToOrganizationId()));
        events.publishEvent(new MemberTransferRequested(saved.getId(), memberId, member.getFullName(), target.getId(),
                target.getName(), currentUser.memberId()));
        return responses(List.of(saved)).of(saved, currentUser);
    }

    @Transactional
    public MemberTransferResponse approve(String id, String note, CurrentUser currentUser) {
        MemberTransfer transfer = getPendingTransfer(id, currentUser);
        requireDecide(transfer, currentUser);
        Member member = getMember(transfer.getMemberId());
        OrganizationUnit target = organizationUnitRepository.findById(transfer.getToOrganizationId()).orElseThrow();
        String oldOrganizationId = member.getOrganization().getId();

        member.setOrganization(target);
        MemberRole oldRole = member.getMemberRole();
        // a TDP officer's role belongs to the TDP they leave
        if (oldRole == MemberRole.TDP_SECRETARY || oldRole == MemberRole.TDP_DEPUTY_SECRETARY) {
            member.setMemberRole(MemberRole.MEMBER);
            recordRoleChange(member, oldRole, currentUser);
        }
        // scope is taken from the member at token issue: end sessions so the new TDP applies right away
        if (member.getUserId() != null) {
            refreshTokenService.revokeAllForUser(member.getUserId());
        }
        transfer.decide(TransferStatus.APPROVED, currentUser.memberId(), blankToNull(note));

        auditLogService.record(currentUser, AuditAction.APPROVE_TRANSFER, ResourceType.TRANSFER, transfer.getId(),
                target.getId(), AuditDiff.snapshot("memberId", member.getId(), "note", transfer.getDecisionNote()));
        auditLogService.record(currentUser, AuditAction.CHANGE_ORGANIZATION, ResourceType.MEMBER, member.getId(), target.getId(),
                AuditDiff.changes(AuditDiff.snapshot("organizationId", oldOrganizationId), AuditDiff.snapshot("organizationId", target.getId())));
        events.publishEvent(new MemberTransferDecided(transfer.getId(), member.getId(), true, target.getName()));
        return responses(List.of(transfer)).of(transfer, currentUser);
    }

    @Transactional
    public MemberTransferResponse reject(String id, String note, CurrentUser currentUser) {
        MemberTransfer transfer = getPendingTransfer(id, currentUser);
        requireDecide(transfer, currentUser);
        transfer.decide(TransferStatus.REJECTED, currentUser.memberId(), blankToNull(note));
        auditLogService.record(currentUser, AuditAction.REJECT_TRANSFER, ResourceType.TRANSFER, transfer.getId(),
                transfer.getToOrganizationId(), AuditDiff.snapshot("memberId", transfer.getMemberId(), "note", transfer.getDecisionNote()));
        String targetName = organizationUnitRepository.findById(transfer.getToOrganizationId()).map(OrganizationUnit::getName).orElse("");
        events.publishEvent(new MemberTransferDecided(transfer.getId(), transfer.getMemberId(), false, targetName));
        return responses(List.of(transfer)).of(transfer, currentUser);
    }

    @Transactional
    public MemberTransferResponse cancel(String id, CurrentUser currentUser) {
        MemberTransfer transfer = getPendingTransfer(id, currentUser);
        if (!canCancel(transfer, currentUser)) {
            throw OrgScopePolicy.outOfScope("Only the requester, the member or a ward officer can cancel this transfer");
        }
        transfer.decide(TransferStatus.CANCELLED, currentUser.memberId(), null);
        return responses(List.of(transfer)).of(transfer, currentUser);
    }

    /** Records a TDP change made directly on the profile, so the member's history stays complete. */
    @Transactional
    public void recordDirectChange(Member member, String fromOrganizationId, CurrentUser currentUser) {
        transferRepository.save(MemberTransfer.direct(member.getId(), fromOrganizationId, member.getOrganization().getId(), currentUser.memberId()));
    }

    private static boolean canDecide(MemberTransfer transfer, CurrentUser currentUser) {
        return transfer.getStatus() == TransferStatus.PENDING
                && (currentUser.hasWardScope()
                || (currentUser.hasTdpScope() && OrgScopePolicy.isOwnTdp(currentUser, transfer.getToOrganizationId())));
    }

    private static boolean canCancel(MemberTransfer transfer, CurrentUser currentUser) {
        String me = currentUser.memberId();
        return transfer.getStatus() == TransferStatus.PENDING
                && (currentUser.hasWardScope()
                || (me != null && (me.equals(transfer.getMemberId()) || me.equals(transfer.getRequestedBy()))));
    }

    private void requireDecide(MemberTransfer transfer, CurrentUser currentUser) {
        if (!canDecide(transfer, currentUser)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "TRANSFER_DECISION_FORBIDDEN", "Current user cannot decide this transfer");
        }
    }

    private void recordRoleChange(Member member, MemberRole oldRole, CurrentUser currentUser) {
        MemberRoleChangeAudit audit = new MemberRoleChangeAudit();
        audit.setMemberId(member.getId());
        audit.setUserId(member.getUserId());
        audit.setOldRole(oldRole);
        audit.setNewRole(member.getMemberRole());
        audit.setActorUserId(currentUser.userId());
        audit.setActorMemberId(currentUser.memberId());
        roleChangeAuditRepository.save(audit);
        auditLogService.record(currentUser, AuditAction.CHANGE_ROLE, ResourceType.MEMBER, member.getId(), member.getOrganization().getId(),
                AuditDiff.changes(AuditDiff.snapshot("memberRole", oldRole), AuditDiff.snapshot("memberRole", member.getMemberRole())));
    }

    private MemberTransfer getVisibleTransfer(String id, CurrentUser currentUser) {
        MemberTransfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TRANSFER_NOT_FOUND", "Transfer not found"));
        boolean visible = currentUser.hasWardScope()
                || (currentUser.hasTdpScope() && (OrgScopePolicy.isOwnTdp(currentUser, transfer.getFromOrganizationId())
                || OrgScopePolicy.isOwnTdp(currentUser, transfer.getToOrganizationId())))
                || transfer.getMemberId().equals(currentUser.memberId());
        if (!visible) {
            throw OrgScopePolicy.outOfScope("Transfer is outside current user's scope");
        }
        return transfer;
    }

    private MemberTransfer getPendingTransfer(String id, CurrentUser currentUser) {
        MemberTransfer transfer = getVisibleTransfer(id, currentUser);
        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "TRANSFER_NOT_PENDING", "Transfer was already decided");
        }
        return transfer;
    }

    private Member getMember(String id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Member not found"));
    }

    private Responses responses(List<MemberTransfer> transfers) {
        NameLookup.Names names = nameLookup.of(
                transfers.stream().flatMap(t -> Stream.of(t.getMemberId(), t.getRequestedBy(), t.getDecidedBy())).toList(),
                transfers.stream().flatMap(t -> Stream.of(t.getFromOrganizationId(), t.getToOrganizationId())).toList());
        Map<String, MemberRoster.Card> cards = memberRoster.cards(transfers.stream().map(MemberTransfer::getMemberId).toList());
        return new Responses(names, cards);
    }

    private record Responses(NameLookup.Names names, Map<String, MemberRoster.Card> cards) {

        MemberTransferResponse of(MemberTransfer transfer, CurrentUser currentUser) {
            MemberRoster.Card card = cards.get(transfer.getMemberId());
            return new MemberTransferResponse(
                    transfer.getId(),
                    transfer.getMemberId(),
                    names.member(transfer.getMemberId()),
                    card == null ? null : card.avatarUrl(),
                    transfer.getFromOrganizationId(),
                    names.organization(transfer.getFromOrganizationId()),
                    transfer.getToOrganizationId(),
                    names.organization(transfer.getToOrganizationId()),
                    transfer.getReason(),
                    transfer.getStatus(),
                    transfer.getRequestedBy(),
                    names.member(transfer.getRequestedBy()),
                    transfer.getRequestedAt(),
                    transfer.getDecidedBy(),
                    names.member(transfer.getDecidedBy()),
                    transfer.getDecidedAt(),
                    transfer.getDecisionNote(),
                    canDecide(transfer, currentUser),
                    canCancel(transfer, currentUser)
            );
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
