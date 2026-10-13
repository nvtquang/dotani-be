package com.dotani.emulation.service;

import com.dotani.audit.entity.AuditAction;
import com.dotani.audit.entity.ResourceType;
import com.dotani.audit.service.AuditDiff;
import com.dotani.audit.service.AuditLogService;
import com.dotani.emulation.dto.EmulationDtos.AwardRequest;
import com.dotani.emulation.dto.EmulationDtos.AwardResponse;
import com.dotani.emulation.dto.EmulationDtos.BoardRow;
import com.dotani.emulation.dto.EmulationDtos.ProfileResponse;
import com.dotani.emulation.dto.EmulationDtos.RatingRequest;
import com.dotani.emulation.dto.EmulationDtos.RatingResponse;
import com.dotani.emulation.dto.EmulationDtos.SummaryResponse;
import com.dotani.emulation.entity.AwardKind;
import com.dotani.emulation.entity.MemberAward;
import com.dotani.emulation.entity.MemberRating;
import com.dotani.emulation.entity.RatingLevel;
import com.dotani.emulation.repository.MemberAwardRepository;
import com.dotani.emulation.repository.MemberRatingRepository;
import com.dotani.member.entity.Member;
import com.dotani.member.entity.MemberStatus;
import com.dotani.member.repository.MemberRepository;
import com.dotani.member.repository.MemberSpecifications;
import com.dotani.member.service.MemberScopeService;
import com.dotani.member.service.NameLookup;
import com.dotani.shared.exception.ApiException;
import com.dotani.shared.security.CurrentUser;
import com.dotani.shared.time.AppTime;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hồ sơ thi đua. A member reads their own record; officers read and maintain the records of the members
 * they manage (same rules as editing the member profile: TDP officers cannot touch ward officers).
 */
@Service
public class EmulationService {

    static final int FIRST_YEAR = 2000;

    private final MemberAwardRepository awardRepository;
    private final MemberRatingRepository ratingRepository;
    private final MemberRepository memberRepository;
    private final MemberScopeService memberScopeService;
    private final NameLookup nameLookup;
    private final AuditLogService auditLogService;

    public EmulationService(
            MemberAwardRepository awardRepository,
            MemberRatingRepository ratingRepository,
            MemberRepository memberRepository,
            MemberScopeService memberScopeService,
            NameLookup nameLookup,
            AuditLogService auditLogService
    ) {
        this.awardRepository = awardRepository;
        this.ratingRepository = ratingRepository;
        this.memberRepository = memberRepository;
        this.memberScopeService = memberScopeService;
        this.nameLookup = nameLookup;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public ProfileResponse profile(String memberId, CurrentUser currentUser) {
        Member member = getMember(memberId);
        memberScopeService.requireRead(currentUser, member);
        List<MemberRating> ratings = ratingRepository.findByMemberIdOrderByYearDesc(memberId);
        List<MemberAward> awards = awardRepository.findByMemberIdOrderByDecisionDateDescCreatedAtDesc(memberId);
        NameLookup.Names names = nameLookup.of(
                Stream.concat(ratings.stream().map(MemberRating::getRatedBy), awards.stream().map(MemberAward::getCreatedBy)).toList(),
                List.of());
        return new ProfileResponse(
                member.getId(),
                member.getFullName(),
                member.getOrganization().getName(),
                !currentUser.isMember() && memberScopeService.canWrite(currentUser, member),
                ratings.stream().map(rating -> toResponse(rating, names)).toList(),
                awards.stream().map(award -> toResponse(award, names)).toList()
        );
    }

    @Transactional
    public AwardResponse createAward(AwardRequest request, CurrentUser currentUser) {
        Member member = getManagedMember(request.memberId(), currentUser);
        MemberAward award = new MemberAward();
        award.setMemberId(member.getId());
        award.setCreatedBy(currentUser.memberId());
        apply(award, request);
        MemberAward saved = awardRepository.save(award);
        auditLogService.record(currentUser, AuditAction.CREATE_AWARD, ResourceType.AWARD, saved.getId(),
                member.getOrganization().getId(), AuditDiff.created(auditSnapshot(saved)));
        return toResponse(saved, names(saved.getCreatedBy()));
    }

    @Transactional
    public AwardResponse updateAward(String id, AwardRequest request, CurrentUser currentUser) {
        MemberAward award = getAward(id);
        Member member = getManagedMember(award.getMemberId(), currentUser);
        if (!award.getMemberId().equals(request.memberId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "An award cannot move to another member");
        }
        Map<String, Object> before = auditSnapshot(award);
        apply(award, request);
        auditLogService.record(currentUser, AuditAction.UPDATE_AWARD, ResourceType.AWARD, award.getId(),
                member.getOrganization().getId(), AuditDiff.changes(before, auditSnapshot(award)));
        return toResponse(award, names(award.getCreatedBy()));
    }

    @Transactional
    public void deleteAward(String id, CurrentUser currentUser) {
        MemberAward award = getAward(id);
        Member member = getManagedMember(award.getMemberId(), currentUser);
        auditLogService.record(currentUser, AuditAction.DELETE_AWARD, ResourceType.AWARD, award.getId(),
                member.getOrganization().getId(), AuditDiff.deleted(auditSnapshot(award)));
        awardRepository.delete(award);
    }

    /** Sets the member's rating for the year, replacing an earlier one. */
    @Transactional
    public RatingResponse rate(RatingRequest request, CurrentUser currentUser) {
        validateYear(request.year());
        Member member = getManagedMember(request.memberId(), currentUser);
        MemberRating rating = ratingRepository.findByMemberIdAndYear(member.getId(), request.year()).orElseGet(MemberRating::new);
        String before = rating.getRating() == null ? null : rating.getRating().name();
        rating.setMemberId(member.getId());
        rating.setYear(request.year());
        rating.setRating(request.rating());
        rating.setNote(blankToNull(request.note()));
        rating.setRatedBy(currentUser.memberId());
        MemberRating saved = ratingRepository.saveAndFlush(rating);
        auditLogService.record(currentUser, AuditAction.RATE_MEMBER, ResourceType.RATING, saved.getId(), member.getOrganization().getId(),
                AuditDiff.changes(AuditDiff.snapshot("memberId", null, "year", null, "rating", before),
                        AuditDiff.snapshot("memberId", member.getId(), "year", saved.getYear(), "rating", saved.getRating())));
        return toResponse(saved, names(saved.getRatedBy()));
    }

    @Transactional
    public void deleteRating(String id, CurrentUser currentUser) {
        MemberRating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RATING_NOT_FOUND", "Rating not found"));
        Member member = getManagedMember(rating.getMemberId(), currentUser);
        auditLogService.record(currentUser, AuditAction.DELETE_RATING, ResourceType.RATING, rating.getId(), member.getOrganization().getId(),
                AuditDiff.deleted(AuditDiff.snapshot("memberId", member.getId(), "year", rating.getYear(), "rating", rating.getRating())));
        ratingRepository.delete(rating);
    }

    /** Active members the officer oversees with their rating for the year, by name. */
    @Transactional(readOnly = true)
    public Page<BoardRow> board(int year, String organizationId, String keyword, Pageable pageable, CurrentUser currentUser) {
        requireOfficer(currentUser);
        validateYear(year);
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("fullName", "id"));
        Page<Member> members = memberRepository.findAll(boardMembers(organizationId, currentUser).and(MemberSpecifications.keywordContains(keyword)), sorted);
        Map<String, MemberRating> ratings = ratingRepository.findByYearAndMemberIdIn(year, members.map(Member::getId).getContent())
                .stream().collect(Collectors.toMap(MemberRating::getMemberId, Function.identity()));
        return members.map(member -> {
            MemberRating rating = ratings.get(member.getId());
            return new BoardRow(
                    member.getId(),
                    member.getFullName(),
                    member.getOrganization().getId(),
                    member.getOrganization().getName(),
                    member.getAvatarUrl(),
                    rating == null ? null : rating.getId(),
                    rating == null ? null : rating.getRating(),
                    rating == null ? null : rating.getNote(),
                    memberScopeService.canWrite(currentUser, member)
            );
        });
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary(int year, String organizationId, CurrentUser currentUser) {
        requireOfficer(currentUser);
        validateYear(year);
        List<String> memberIds = memberRepository.findAll(boardMembers(organizationId, currentUser)).stream().map(Member::getId).toList();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (RatingLevel level : RatingLevel.values()) {
            counts.put(level.name(), 0L);
        }
        List<MemberRating> ratings = memberIds.isEmpty() ? List.of() : ratingRepository.findByYearAndMemberIdIn(year, memberIds);
        ratings.forEach(rating -> counts.merge(rating.getRating().name(), 1L, Long::sum));
        counts.put("UNRATED", (long) memberIds.size() - ratings.size());
        List<MemberAward> awards = memberIds.isEmpty()
                ? List.of()
                : awardRepository.findByMemberIdInAndDecisionDateBetween(memberIds, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        long commendations = awards.stream().filter(award -> award.getForm().kind() == AwardKind.COMMENDATION).count();
        return new SummaryResponse(year, memberIds.size(), counts, commendations, awards.size() - commendations);
    }

    private Specification<Member> boardMembers(String organizationId, CurrentUser currentUser) {
        return Specification
                .where(MemberSpecifications.withinScope(currentUser))
                .and(MemberSpecifications.hasOrganization(organizationId))
                .and(MemberSpecifications.hasStatus(MemberStatus.ACTIVE));
    }

    private void apply(MemberAward award, AwardRequest request) {
        award.setForm(request.form());
        award.setTitle(request.title().trim());
        award.setReason(blankToNull(request.reason()));
        award.setDecisionNumber(blankToNull(request.decisionNumber()));
        award.setDecisionDate(request.decisionDate());
        award.setIssuer(blankToNull(request.issuer()));
    }

    private AwardResponse toResponse(MemberAward award, NameLookup.Names names) {
        return new AwardResponse(
                award.getId(),
                award.getMemberId(),
                award.getForm(),
                award.getForm().kind(),
                award.getTitle(),
                award.getReason(),
                award.getDecisionNumber(),
                award.getDecisionDate(),
                award.getIssuer(),
                award.getCreatedBy(),
                names.member(award.getCreatedBy()),
                award.getCreatedAt()
        );
    }

    private RatingResponse toResponse(MemberRating rating, NameLookup.Names names) {
        return new RatingResponse(
                rating.getId(),
                rating.getMemberId(),
                rating.getYear(),
                rating.getRating(),
                rating.getNote(),
                rating.getRatedBy(),
                names.member(rating.getRatedBy()),
                rating.getUpdatedAt()
        );
    }

    private NameLookup.Names names(String memberId) {
        return nameLookup.of(Collections.singletonList(memberId), List.of());
    }

    private Member getMember(String id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Member not found"));
    }

    private Member getManagedMember(String memberId, CurrentUser currentUser) {
        requireOfficer(currentUser);
        Member member = getMember(memberId);
        memberScopeService.requireWrite(currentUser, member);
        return member;
    }

    private MemberAward getAward(String id) {
        return awardRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AWARD_NOT_FOUND", "Award not found"));
    }

    private static void requireOfficer(CurrentUser currentUser) {
        if (currentUser.isMember()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMULATION_FORBIDDEN", "Only officers manage emulation records");
        }
    }

    private static void validateYear(Integer year) {
        if (year == null || year < FIRST_YEAR || year > AppTime.today().getYear()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RATING_YEAR", "Rating year is out of range");
        }
    }

    private static Map<String, Object> auditSnapshot(MemberAward award) {
        return AuditDiff.snapshot(
                "memberId", award.getMemberId(),
                "form", award.getForm(),
                "title", award.getTitle(),
                "decisionNumber", award.getDecisionNumber(),
                "decisionDate", award.getDecisionDate()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
