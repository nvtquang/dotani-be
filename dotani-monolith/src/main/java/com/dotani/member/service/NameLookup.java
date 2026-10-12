package com.dotani.member.service;

import com.dotani.member.repository.OrganizationUnitRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves member and organization names for API responses in one round trip per kind, so lists can
 * show "who" and "where" without the frontend fetching each member or organization.
 */
@Service
public class NameLookup {

    public record Names(Map<String, String> members, Map<String, String> organizations) {

        public String member(String memberId) {
            return memberId == null ? null : members.get(memberId);
        }

        public String organization(String organizationId) {
            return organizationId == null ? null : organizations.get(organizationId);
        }
    }

    private final MemberDirectoryService memberDirectoryService;
    private final OrganizationUnitRepository organizationUnitRepository;

    public NameLookup(MemberDirectoryService memberDirectoryService, OrganizationUnitRepository organizationUnitRepository) {
        this.memberDirectoryService = memberDirectoryService;
        this.organizationUnitRepository = organizationUnitRepository;
    }

    @Transactional(readOnly = true)
    public Names of(Collection<String> memberIds, Collection<String> organizationIds) {
        Set<String> orgIds = organizationIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, String> organizations = orgIds.isEmpty()
                ? Map.of()
                : organizationUnitRepository.findAllById(orgIds).stream()
                        .collect(Collectors.toMap(unit -> unit.getId(), unit -> unit.getName(), (left, right) -> left));
        // includes deleted members: old posts and messages still show who wrote them
        return new Names(memberDirectoryService.findDisplayNames(memberIds), organizations);
    }
}
