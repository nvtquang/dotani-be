package com.dotani.member.service;

import com.dotani.member.entity.MemberStatus;
import com.dotani.member.repository.MemberRepository;
import com.dotani.shared.security.CurrentUser;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who belongs where, for modules that address "the members of an organization" (fees, polls, reports):
 * a ward reaches every member of its TDPs, a TDP only its own members.
 */
@Service
public class MemberRoster {

    /** Where an active member sits: their TDP (or ward) and its parent. */
    public record Placement(String memberId, String organizationId, String parentId) {

        /** True when the member belongs to the organization or to one of its TDPs. */
        public boolean in(String targetOrganizationId) {
            return targetOrganizationId != null
                    && (targetOrganizationId.equals(organizationId) || targetOrganizationId.equals(parentId));
        }
    }

    /** Public card of a member, deleted members included (authors of old comments). */
    public record Card(String id, String fullName, String avatarUrl) {
    }

    private final MemberRepository memberRepository;

    public MemberRoster(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<Placement> activePlacements() {
        return memberRepository.findPlacementsByStatus(MemberStatus.ACTIVE).stream()
                .map(row -> new Placement((String) row[0], (String) row[1], (String) row[2]))
                .toList();
    }

    /**
     * Active members of the organization tree that the caller oversees: everyone for ward officers,
     * only the caller's TDP for TDP officers.
     */
    public static List<Placement> overseenBy(CurrentUser user, List<Placement> placements, String organizationId) {
        return placements.stream()
                .filter(placement -> placement.in(organizationId))
                .filter(placement -> user.hasWardScope() || Objects.equals(placement.organizationId(), user.tdpId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> activeMemberIdsIn(String organizationId) {
        return memberRepository.findIdsInOrganizationTreeAndStatus(organizationId, MemberStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public boolean isActiveIn(String memberId, String organizationId) {
        return memberId != null && activeMemberIdsIn(organizationId).contains(memberId);
    }

    @Transactional(readOnly = true)
    public Map<String, Card> cards(Collection<String> memberIds) {
        Set<String> ids = memberIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            return Map.of();
        }
        return memberRepository.findCardsIncludingDeleted(ids).stream()
                .map(row -> new Card((String) row[0], (String) row[1], (String) row[2]))
                .collect(Collectors.toMap(Card::id, card -> card, (left, right) -> left));
    }
}
