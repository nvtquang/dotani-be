package com.dotani.fee.service;

import com.dotani.audit.entity.AuditAction;
import com.dotani.audit.entity.ResourceType;
import com.dotani.audit.service.AuditDiff;
import com.dotani.audit.service.AuditLogService;
import com.dotani.fee.dto.FeeMemberStatusResponse;
import com.dotani.fee.dto.FeePaymentRequest;
import com.dotani.fee.dto.FeePeriodRequest;
import com.dotani.fee.dto.FeePeriodResponse;
import com.dotani.fee.entity.FeePayment;
import com.dotani.fee.entity.FeePeriod;
import com.dotani.fee.entity.FeePeriodStatus;
import com.dotani.fee.repository.FeePaymentRepository;
import com.dotani.fee.repository.FeePeriodRepository;
import com.dotani.member.entity.Member;
import com.dotani.member.repository.MemberRepository;
import com.dotani.member.service.MemberRoster;
import com.dotani.member.service.MemberRoster.Placement;
import com.dotani.member.service.NameLookup;
import com.dotani.member.service.OrganizationDirectory;
import com.dotani.member.service.StorageService;
import com.dotani.shared.events.DomainEvents.FeeReminderRequested;
import com.dotani.shared.exception.ApiException;
import com.dotani.shared.persistence.TextSearch;
import com.dotani.shared.security.CurrentUser;
import com.dotani.shared.security.OrgScopePolicy;
import com.dotani.shared.web.Pages;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đoàn phí. Everyone sees the periods of their ward and of their own TDP and their own payment status.
 * Ward officers manage every period and every member's payment; TDP officers manage their TDP's periods
 * and record payments of their TDP's members (also for ward-wide periods).
 */
@Service
public class FeeService {

    static final Duration REMINDER_COOLDOWN = Duration.ofHours(1);

    private final FeePeriodRepository periodRepository;
    private final FeePaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final MemberRoster memberRoster;
    private final NameLookup nameLookup;
    private final OrganizationDirectory organizationDirectory;
    private final StorageService storageService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher events;

    public FeeService(
            FeePeriodRepository periodRepository,
            FeePaymentRepository paymentRepository,
            MemberRepository memberRepository,
            MemberRoster memberRoster,
            NameLookup nameLookup,
            OrganizationDirectory organizationDirectory,
            StorageService storageService,
            AuditLogService auditLogService,
            ApplicationEventPublisher events
    ) {
        this.periodRepository = periodRepository;
        this.paymentRepository = paymentRepository;
        this.memberRepository = memberRepository;
        this.memberRoster = memberRoster;
        this.nameLookup = nameLookup;
        this.organizationDirectory = organizationDirectory;
        this.storageService = storageService;
        this.auditLogService = auditLogService;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Page<FeePeriodResponse> findAll(FeePeriodStatus status, Pageable pageable, CurrentUser currentUser) {
        Specification<FeePeriod> specification = Specification
                .where(FeePeriodRepository.visibleTo(currentUser))
                .and(FeePeriodRepository.hasStatus(status));
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<FeePeriod> page = periodRepository.findAll(specification, sorted);
        return toResponses(page.getContent(), currentUser, page);
    }

    @Transactional(readOnly = true)
    public FeePeriodResponse findById(String id, CurrentUser currentUser) {
        FeePeriod period = getReadablePeriod(id, currentUser);
        return toResponses(List.of(period), currentUser, null).getContent().get(0);
    }

    @Transactional
    public FeePeriodResponse create(FeePeriodRequest request, CurrentUser currentUser) {
        requireManage(currentUser, request.organizationId());
        FeePeriod period = new FeePeriod();
        apply(period, request);
        period.setCreatedBy(currentUser.memberId());
        FeePeriod saved = periodRepository.save(period);
        auditLogService.record(currentUser, AuditAction.CREATE_FEE_PERIOD, ResourceType.FEE_PERIOD, saved.getId(),
                saved.getOrganizationId(), AuditDiff.created(auditSnapshot(saved)));
        return findById(saved.getId(), currentUser);
    }

    @Transactional
    public FeePeriodResponse update(String id, FeePeriodRequest request, CurrentUser currentUser) {
        FeePeriod period = getPeriod(id);
        requireManage(currentUser, period.getOrganizationId());
        requireManage(currentUser, request.organizationId());
        Map<String, Object> before = auditSnapshot(period);
        apply(period, request);
        auditLogService.record(currentUser, AuditAction.UPDATE_FEE_PERIOD, ResourceType.FEE_PERIOD, period.getId(),
                period.getOrganizationId(), AuditDiff.changes(before, auditSnapshot(period)));
        return findById(id, currentUser);
    }

    @Transactional
    public void delete(String id, CurrentUser currentUser) {
        FeePeriod period = getPeriod(id);
        requireManage(currentUser, period.getOrganizationId());
        if (paymentRepository.existsByPeriodId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "FEE_PERIOD_HAS_PAYMENTS", "Fee period already has payments");
        }
        auditLogService.record(currentUser, AuditAction.DELETE_FEE_PERIOD, ResourceType.FEE_PERIOD, period.getId(),
                period.getOrganizationId(), AuditDiff.deleted(auditSnapshot(period)));
        periodRepository.delete(period);
    }

    /** Members owing the period that the caller oversees, with their payment, filtered by paid state and name. */
    @Transactional(readOnly = true)
    public Page<FeeMemberStatusResponse> findMembers(String id, Boolean paid, String keyword, Pageable pageable, CurrentUser currentUser) {
        FeePeriod period = getReadablePeriod(id, currentUser);
        requireOfficer(currentUser);
        Map<String, FeePayment> payments = paymentsByMember(paymentRepository.findByPeriodId(id));
        List<String> memberIds = MemberRoster.overseenBy(currentUser, memberRoster.activePlacements(), period.getOrganizationId())
                .stream().map(Placement::memberId).toList();
        List<Member> members = memberRepository.findAllById(memberIds).stream()
                .filter(member -> TextSearch.matches(member.getFullName(), keyword))
                .filter(member -> paid == null || paid == payments.containsKey(member.getId()))
                .sorted(Comparator.comparing(Member::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        NameLookup.Names recorders = nameLookup.of(payments.values().stream().map(FeePayment::getRecordedBy).toList(), List.of());
        List<FeeMemberStatusResponse> rows = members.stream()
                .map(member -> toMemberStatus(member, payments.get(member.getId()), recorders))
                .toList();
        return Pages.of(rows, pageable);
    }

    @Transactional
    public FeeMemberStatusResponse recordPayment(String id, FeePaymentRequest request, CurrentUser currentUser) {
        FeePeriod period = getReadablePeriod(id, currentUser);
        requireOfficer(currentUser);
        if (period.getStatus() != FeePeriodStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "FEE_PERIOD_CLOSED", "Fee period is closed");
        }
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Member not found"));
        requireOverseesMember(currentUser, member);
        if (!belongsTo(member, period.getOrganizationId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FEE_MEMBER_NOT_IN_PERIOD", "Member does not owe this fee period");
        }
        if (paymentRepository.findByPeriodIdAndMemberId(id, member.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "FEE_ALREADY_PAID", "Member already paid this period");
        }

        FeePayment payment = new FeePayment();
        payment.setPeriodId(id);
        payment.setMemberId(member.getId());
        payment.setAmount(request.amount() == null ? period.getAmount() : request.amount());
        payment.setMethod(request.method());
        payment.setPaidAt(request.paidAt() == null ? Instant.now() : request.paidAt());
        payment.setNote(blankToNull(request.note()));
        payment.setRecordedBy(currentUser.memberId());
        FeePayment saved = paymentRepository.saveAndFlush(payment);
        auditLogService.record(currentUser, AuditAction.RECORD_FEE_PAYMENT, ResourceType.FEE_PERIOD, period.getId(),
                period.getOrganizationId(), AuditDiff.snapshot("memberId", member.getId(), "amount", saved.getAmount(), "method", saved.getMethod()));
        return toMemberStatus(member, saved, nameLookup.of(Collections.singletonList(saved.getRecordedBy()), List.of()));
    }

    @Transactional
    public void cancelPayment(String id, String paymentId, CurrentUser currentUser) {
        FeePeriod period = getReadablePeriod(id, currentUser);
        requireOfficer(currentUser);
        FeePayment payment = paymentRepository.findByIdAndPeriodId(paymentId, id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FEE_PAYMENT_NOT_FOUND", "Fee payment not found"));
        // a deleted member's payment can still be cancelled by ward officers
        memberRepository.findById(payment.getMemberId()).ifPresentOrElse(
                member -> requireOverseesMember(currentUser, member),
                () -> requireManage(currentUser, period.getOrganizationId()));
        auditLogService.record(currentUser, AuditAction.CANCEL_FEE_PAYMENT, ResourceType.FEE_PERIOD, period.getId(),
                period.getOrganizationId(), AuditDiff.snapshot("memberId", payment.getMemberId(), "amount", payment.getAmount()));
        paymentRepository.delete(payment);
    }

    /** Notifies the unpaid members the caller oversees; returns how many were reminded. */
    @Transactional
    public int remindUnpaid(String id, CurrentUser currentUser) {
        FeePeriod period = getReadablePeriod(id, currentUser);
        requireOfficer(currentUser);
        if (period.getStatus() != FeePeriodStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "FEE_PERIOD_CLOSED", "Fee period is closed");
        }
        Instant now = Instant.now();
        if (period.getLastRemindedAt() != null && period.getLastRemindedAt().plus(REMINDER_COOLDOWN).isAfter(now)) {
            long minutes = Math.max(1, Duration.between(now, period.getLastRemindedAt().plus(REMINDER_COOLDOWN)).toMinutes());
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "FEE_REMINDER_TOO_SOON", "Reminder sent recently", minutes);
        }
        Map<String, FeePayment> payments = paymentsByMember(paymentRepository.findByPeriodId(id));
        List<String> unpaid = MemberRoster.overseenBy(currentUser, memberRoster.activePlacements(), period.getOrganizationId())
                .stream()
                .map(Placement::memberId)
                .filter(memberId -> !payments.containsKey(memberId))
                .toList();
        if (unpaid.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "FEE_NOTHING_TO_REMIND", "Every member has paid");
        }
        period.markReminded(now);
        events.publishEvent(new FeeReminderRequested(period.getId(), period.getName(), period.getAmount(), period.getDueDate(), unpaid));
        return unpaid.size();
    }

    /** The collector's QR Banking image, for anyone who can see the period (they transfer to it). */
    @Transactional(readOnly = true)
    public Resource loadCollectorQr(String id, CurrentUser currentUser) {
        FeePeriod period = getReadablePeriod(id, currentUser);
        String qrUrl = period.getCollectorMemberId() == null
                ? null
                : memberRepository.findById(period.getCollectorMemberId()).map(Member::getBankQrImageUrl).orElse(null);
        if (qrUrl == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FEE_COLLECTOR_QR_NOT_FOUND", "Collector has no banking QR");
        }
        return storageService.loadBankQr(qrUrl);
    }

    private Page<FeePeriodResponse> toResponses(List<FeePeriod> periods, CurrentUser currentUser, Page<FeePeriod> page) {
        List<String> periodIds = periods.stream().map(FeePeriod::getId).toList();
        Map<String, List<FeePayment>> paymentsByPeriod = periodIds.isEmpty()
                ? Map.of()
                : paymentRepository.findByPeriodIdIn(periodIds).stream().collect(Collectors.groupingBy(FeePayment::getPeriodId));
        List<Placement> placements = memberRoster.activePlacements();
        Map<String, Member> collectors = memberRepository.findAllById(
                        periods.stream().map(FeePeriod::getCollectorMemberId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Member::getId, Function.identity()));
        NameLookup.Names names = nameLookup.of(List.of(), periods.stream().map(FeePeriod::getOrganizationId).toList());
        Placement me = placements.stream().filter(placement -> placement.memberId().equals(currentUser.memberId())).findFirst().orElse(null);

        List<FeePeriodResponse> responses = periods.stream().map(period -> {
            Map<String, FeePayment> payments = paymentsByMember(paymentsByPeriod.getOrDefault(period.getId(), List.of()));
            FeePayment mine = currentUser.memberId() == null ? null : payments.get(currentUser.memberId());
            boolean applies = mine != null || (me != null && me.in(period.getOrganizationId()));
            return new FeePeriodResponse(
                    period.getId(),
                    period.getName(),
                    period.getDescription(),
                    period.getOrganizationId(),
                    names.organization(period.getOrganizationId()),
                    period.getAmount(),
                    period.getDueDate(),
                    period.getStatus(),
                    toCollector(period, collectors.get(period.getCollectorMemberId())),
                    currentUser.isMember() ? null : summary(period, payments, MemberRoster.overseenBy(currentUser, placements, period.getOrganizationId())),
                    applies ? new FeePeriodResponse.MyPayment(mine != null, mine == null ? period.getAmount() : mine.getAmount(),
                            mine == null ? null : mine.getMethod(), mine == null ? null : mine.getPaidAt()) : null,
                    canManage(currentUser, period.getOrganizationId()),
                    period.getLastRemindedAt(),
                    period.getCreatedAt()
            );
        }).toList();
        return page == null ? Pages.of(responses, Pageable.unpaged()) : new PageImpl<>(responses, page.getPageable(), page.getTotalElements());
    }

    private FeePeriodResponse.Summary summary(FeePeriod period, Map<String, FeePayment> payments, List<Placement> overseen) {
        long expected = overseen.size();
        long paid = overseen.stream().filter(placement -> payments.containsKey(placement.memberId())).count();
        // collected money includes payments of members who left or were deleted since they paid
        Collection<String> overseenIds = overseen.stream().map(Placement::memberId).collect(Collectors.toSet());
        long collected = payments.values().stream()
                .filter(payment -> overseenIds.contains(payment.getMemberId()))
                .mapToLong(FeePayment::getAmount)
                .sum();
        return new FeePeriodResponse.Summary(expected, paid, expected - paid, collected, expected * period.getAmount());
    }

    private FeePeriodResponse.Collector toCollector(FeePeriod period, Member collector) {
        if (collector == null) {
            return null;
        }
        return new FeePeriodResponse.Collector(
                collector.getId(),
                collector.getFullName(),
                collector.getBankName(),
                collector.getBankCode(),
                collector.getAccountNumber(),
                collector.getAccountHolderName(),
                collector.getBankQrImageUrl() == null ? null : "/api/fees/periods/" + period.getId() + "/collector-qr"
        );
    }

    private FeeMemberStatusResponse toMemberStatus(Member member, FeePayment payment, NameLookup.Names recorders) {
        return new FeeMemberStatusResponse(
                member.getId(),
                member.getFullName(),
                member.getOrganization().getId(),
                member.getOrganization().getName(),
                member.getAvatarUrl(),
                payment != null,
                payment == null ? null : payment.getId(),
                payment == null ? null : payment.getAmount(),
                payment == null ? null : payment.getMethod(),
                payment == null ? null : payment.getPaidAt(),
                payment == null ? null : payment.getNote(),
                payment == null ? null : recorders.member(payment.getRecordedBy())
        );
    }

    private void apply(FeePeriod period, FeePeriodRequest request) {
        String organizationId = request.organizationId().trim();
        organizationDirectory.requireExists(organizationId);
        String collectorId = blankToNull(request.collectorMemberId());
        if (collectorId != null) {
            Member collector = memberRepository.findById(collectorId)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "FEE_COLLECTOR_INVALID", "Collector not found"));
            if (!collector.getMemberRole().isWardOfficer() && !belongsTo(collector, organizationId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "FEE_COLLECTOR_INVALID", "Collector is outside the period's organization");
            }
        }
        period.setName(request.name().trim());
        period.setDescription(blankToNull(request.description()));
        period.setOrganizationId(organizationId);
        period.setAmount(request.amount());
        period.setDueDate(request.dueDate());
        period.setCollectorMemberId(collectorId);
        if (request.status() != null) {
            period.setStatus(request.status());
        }
    }

    private static boolean belongsTo(Member member, String organizationId) {
        return organizationId.equals(member.getOrganization().getId())
                || (member.getOrganization().getParent() != null && organizationId.equals(member.getOrganization().getParent().getId()));
    }

    private static Map<String, FeePayment> paymentsByMember(List<FeePayment> payments) {
        return payments.stream().collect(Collectors.toMap(FeePayment::getMemberId, Function.identity(), (left, right) -> left));
    }

    private FeePeriod getPeriod(String id) {
        return periodRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FEE_PERIOD_NOT_FOUND", "Fee period not found"));
    }

    private FeePeriod getReadablePeriod(String id, CurrentUser currentUser) {
        FeePeriod period = getPeriod(id);
        if (!OrgScopePolicy.canSeeContentOf(currentUser, period.getOrganizationId())) {
            throw OrgScopePolicy.outOfScope("Fee period is outside current user's scope");
        }
        return period;
    }

    private static boolean canManage(CurrentUser currentUser, String organizationId) {
        return currentUser.hasWardScope() || (currentUser.hasTdpScope() && OrgScopePolicy.isOwnTdp(currentUser, organizationId));
    }

    private static void requireManage(CurrentUser currentUser, String organizationId) {
        if (!canManage(currentUser, organizationId == null ? null : organizationId.trim())) {
            throw OrgScopePolicy.outOfScope("Fee period is outside current user's scope");
        }
    }

    private static void requireOfficer(CurrentUser currentUser) {
        if (currentUser.isMember()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only officers manage fee payments");
        }
    }

    // TDP officers collect from the members of their own TDP only
    private static void requireOverseesMember(CurrentUser currentUser, Member member) {
        if (!currentUser.hasWardScope() && !OrgScopePolicy.isOwnTdp(currentUser, member.getOrganization().getId())) {
            throw OrgScopePolicy.outOfScope("Member is outside current user's organization scope");
        }
    }

    private static Map<String, Object> auditSnapshot(FeePeriod period) {
        return AuditDiff.snapshot(
                "name", period.getName(),
                "organizationId", period.getOrganizationId(),
                "amount", period.getAmount(),
                "dueDate", period.getDueDate(),
                "collectorMemberId", period.getCollectorMemberId(),
                "status", period.getStatus()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
