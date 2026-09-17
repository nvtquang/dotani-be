package com.hcmcyu.member.service;

import com.hcmcyu.member.dto.MemberSummaryResponse;
import com.hcmcyu.member.dto.OrganizationUnitRequest;
import com.hcmcyu.member.dto.OrganizationUnitResponse;
import com.hcmcyu.member.entity.OrganizationUnit;
import com.hcmcyu.member.entity.OrganizationUnitType;
import com.hcmcyu.member.exception.MemberServiceException;
import com.hcmcyu.member.repository.MemberRepository;
import com.hcmcyu.member.repository.MemberSpecifications;
import com.hcmcyu.member.mapper.OrganizationUnitMapper;
import com.hcmcyu.member.repository.OrganizationUnitRepository;
import com.hcmcyu.member.security.CurrentUser;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private final OrganizationUnitRepository organizationUnitRepository;
    private final MemberRepository memberRepository;
    private final OrganizationUnitMapper organizationUnitMapper;
    private final OrganizationScopeService organizationScopeService;

    public OrganizationService(
            OrganizationUnitRepository organizationUnitRepository,
            MemberRepository memberRepository,
            OrganizationUnitMapper organizationUnitMapper,
            OrganizationScopeService organizationScopeService
    ) {
        this.organizationUnitRepository = organizationUnitRepository;
        this.memberRepository = memberRepository;
        this.organizationUnitMapper = organizationUnitMapper;
        this.organizationScopeService = organizationScopeService;
    }

    @Transactional(readOnly = true)
    public List<OrganizationUnitResponse> findAll(CurrentUser user) {
        return organizationUnitRepository.findAll()
                .stream()
                .filter(organizationUnit -> organizationScopeService.canRead(user, organizationUnit))
                .sorted(Comparator.comparing(OrganizationUnit::getType).thenComparing(OrganizationUnit::getCode))
                .map(organizationUnitMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizationUnitResponse> findPublicBranches() {
        return organizationUnitRepository.findByType(OrganizationUnitType.YOUTH_UNION_BRANCH)
                .stream()
                .filter(OrganizationUnit::isActive)
                .sorted(Comparator.comparing(OrganizationUnit::getCode))
                .map(organizationUnitMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationUnitResponse findById(String id, CurrentUser user) {
        OrganizationUnit organizationUnit = getOrganizationUnit(id);
        organizationScopeService.requireRead(user, organizationUnit);
        return organizationUnitMapper.toResponse(organizationUnit);
    }

    @Transactional
    public OrganizationUnitResponse create(OrganizationUnitRequest request, CurrentUser user) {
        OrganizationUnit organizationUnit = new OrganizationUnit();
        applyRequest(organizationUnit, request);
        organizationScopeService.requireCreate(user, organizationUnit);

        if (organizationUnitRepository.existsByCode(request.code().trim())) {
            throw conflict("Organization code already exists");
        }

        return organizationUnitMapper.toResponse(organizationUnitRepository.save(organizationUnit));
    }

    @Transactional
    public OrganizationUnitResponse update(String id, OrganizationUnitRequest request, CurrentUser user) {
        OrganizationUnit organizationUnit = getOrganizationUnit(id);
        organizationScopeService.requireWrite(user, organizationUnit);

        if (organizationUnitRepository.existsByCodeAndIdNot(request.code().trim(), id)) {
            throw conflict("Organization code already exists");
        }

        applyRequest(organizationUnit, request);
        organizationScopeService.requireWrite(user, organizationUnit);
        return organizationUnitMapper.toResponse(organizationUnit);
    }

    @Transactional
    public void delete(String id, CurrentUser user) {
        OrganizationUnit organizationUnit = getOrganizationUnit(id);
        organizationScopeService.requireWrite(user, organizationUnit);
        organizationUnitRepository.delete(organizationUnit);
    }

    @Transactional(readOnly = true)
    public List<MemberSummaryResponse> findMembers(String id, CurrentUser user) {
        OrganizationUnit organizationUnit = getOrganizationUnit(id);
        organizationScopeService.requireRead(user, organizationUnit);
        return memberRepository.findAll(MemberSpecifications
                        .withinScope(user)
                        .and(MemberSpecifications.hasOrganization(id)))
                .stream()
                .map(member -> new MemberSummaryResponse(
                        member.getId(),
                        member.getFullName(),
                        member.getOrganization().getId()
                ))
                .toList();
    }

    private void applyRequest(OrganizationUnit organizationUnit, OrganizationUnitRequest request) {
        organizationUnit.setName(request.name().trim());
        organizationUnit.setCode(request.code().trim());
        organizationUnit.setType(request.type());
        organizationUnit.setActive(request.active() == null || request.active());
        organizationUnit.setParent(resolveParent(request));
        validateHierarchy(organizationUnit);
    }

    private OrganizationUnit resolveParent(OrganizationUnitRequest request) {
        if (request.parentId() == null || request.parentId().isBlank()) {
            return null;
        }
        return organizationUnitRepository.findById(request.parentId())
                .orElseThrow(() -> notFound("Parent organization unit not found"));
    }

    private void validateHierarchy(OrganizationUnit organizationUnit) {
        if (organizationUnit.getType() == OrganizationUnitType.WARD && organizationUnit.getParent() != null) {
            throw badRequest("Ward organization unit cannot have a parent");
        }
        if (organizationUnit.getType() == OrganizationUnitType.YOUTH_UNION_BRANCH) {
            if (organizationUnit.getParent() == null) {
                throw badRequest("Youth union branch must have a ward parent");
            }
            if (organizationUnit.getParent().getType() != OrganizationUnitType.WARD) {
                throw badRequest("Youth union branch parent must be a ward");
            }
        }
    }

    private OrganizationUnit getOrganizationUnit(String id) {
        return organizationUnitRepository.findById(id)
                .orElseThrow(() -> notFound("Organization unit not found"));
    }

    private MemberServiceException notFound(String message) {
        return new MemberServiceException(HttpStatus.NOT_FOUND, "ORGANIZATION_NOT_FOUND", message);
    }

    private MemberServiceException conflict(String message) {
        return new MemberServiceException(HttpStatus.CONFLICT, "DUPLICATE_ORGANIZATION_CODE", message);
    }

    private MemberServiceException badRequest(String message) {
        return new MemberServiceException(HttpStatus.BAD_REQUEST, "INVALID_ORGANIZATION_HIERARCHY", message);
    }
}
