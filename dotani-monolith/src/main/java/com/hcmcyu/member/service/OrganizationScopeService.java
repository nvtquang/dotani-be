package com.hcmcyu.member.service;

import com.hcmcyu.member.entity.OrganizationUnit;
import com.hcmcyu.member.entity.OrganizationUnitType;
import com.hcmcyu.member.exception.MemberServiceException;
import com.hcmcyu.member.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OrganizationScopeService {

    public boolean canRead(CurrentUser user, OrganizationUnit organizationUnit) {
        if (user.hasWardScope()) {
            return true;
        }
        if (organizationUnit.getType() == OrganizationUnitType.WARD) {
            return true;
        }
        return isOwnTdp(user, organizationUnit);
    }

    public boolean canWrite(CurrentUser user, OrganizationUnit organizationUnit) {
        if (user.hasWardScope()) {
            return true;
        }
        if (user.hasTdpScope()) {
            return isOwnTdp(user, organizationUnit);
        }
        return false;
    }

    public void requireRead(CurrentUser user, OrganizationUnit organizationUnit) {
        if (!canRead(user, organizationUnit)) {
            throw outOfScope();
        }
    }

    public void requireWrite(CurrentUser user, OrganizationUnit organizationUnit) {
        if (!canWrite(user, organizationUnit)) {
            throw outOfScope();
        }
    }

    public void requireCreate(CurrentUser user, OrganizationUnit organizationUnit) {
        if (!user.hasWardScope()) {
            throw new MemberServiceException(
                    HttpStatus.FORBIDDEN,
                    "ACCESS_DENIED",
                    "Only ward-level officers can create organization units"
            );
        }
    }

    public boolean isOwnTdp(CurrentUser user, OrganizationUnit organizationUnit) {
        return user.tdpId() != null
                && organizationUnit.getType() == OrganizationUnitType.YOUTH_UNION_BRANCH
                && user.tdpId().equals(organizationUnit.getId());
    }

    private MemberServiceException outOfScope() {
        return new MemberServiceException(
                HttpStatus.FORBIDDEN,
                "OUT_OF_SCOPE",
                "Organization unit is outside current user's scope"
        );
    }
}

