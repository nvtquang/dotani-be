package com.hcmcyu.member.mapper;

import com.hcmcyu.member.dto.OrganizationUnitResponse;
import com.hcmcyu.member.entity.OrganizationUnit;
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
