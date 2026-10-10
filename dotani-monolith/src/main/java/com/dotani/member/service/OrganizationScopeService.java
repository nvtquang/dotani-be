package com.dotani.member.service;

import com.dotani.member.entity.OrganizationUnit;
import com.dotani.member.entity.OrganizationUnitType;
import com.dotani.shared.exception.ApiException;
import com.dotani.shared.security.CurrentUser;
import com.dotani.shared.security.OrgScopePolicy;
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
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCESS_DENIED",
                    "Only ward-level officers can create organization units"
            );
        }
    }

    public boolean isOwnTdp(CurrentUser user, OrganizationUnit organizationUnit) {
        return organizationUnit.getType() == OrganizationUnitType.YOUTH_UNION_BRANCH
                && OrgScopePolicy.isOwnTdp(user, organizationUnit.getId());
    }

    private ApiException outOfScope() {
        return OrgScopePolicy.outOfScope("Organization unit is outside current user's scope");
    }
}

