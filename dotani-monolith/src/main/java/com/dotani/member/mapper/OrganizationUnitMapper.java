package com.dotani.member.mapper;

import com.dotani.member.dto.OrganizationUnitResponse;
import com.dotani.member.entity.OrganizationUnit;
import org.springframework.stereotype.Component;

@Component
public class OrganizationUnitMapper {

    public OrganizationUnitResponse toResponse(OrganizationUnit organizationUnit) {
        String parentId = organizationUnit.getParent() == null ? null : organizationUnit.getParent().getId();
        return new OrganizationUnitResponse(
                organizationUnit.getId(),
                organizationUnit.getName(),
                organizationUnit.getCode(),
                organizationUnit.getType(),
                parentId,
                organizationUnit.isActive()
        );
    }
}
