package com.dotani.member.service;

import com.dotani.member.entity.Member;
import com.dotani.shared.exception.ApiException;
import com.dotani.shared.security.CurrentUser;
import com.dotani.shared.security.OrgScopePolicy;
import org.springframework.stereotype.Service;

@Service
public class MemberScopeService {

    public void requireRead(CurrentUser currentUser, Member member) {
        if (canRead(currentUser, member)) {
            return;
        }
        throw outOfScope();
    }

    public void requireWrite(CurrentUser currentUser, Member member) {
        if (canWrite(currentUser, member)) {
            return;
        }
        throw outOfScope();
    }

    public boolean canCreateInOrganization(CurrentUser currentUser, String organizationId) {
        if (currentUser.hasWardScope()) {
            return true;
        }
        return currentUser.hasTdpScope() && OrgScopePolicy.isOwnTdp(currentUser, organizationId);
    }

    public void requireCreateInOrganization(CurrentUser currentUser, String organizationId) {
        if (!canCreateInOrganization(currentUser, organizationId)) {
            throw outOfScope();
        }
    }

    private boolean canRead(CurrentUser currentUser, Member member) {
        if (currentUser.hasWardScope()) {
            return true;
        }
        if (currentUser.hasTdpScope()) {
            return OrgScopePolicy.isOwnTdp(currentUser, member.getOrganization().getId());
        }
        return currentUser.isMember()
                && currentUser.memberId() != null
                && currentUser.memberId().equals(member.getId());
    }

    public boolean canWrite(CurrentUser currentUser, Member member) {
        if (currentUser.hasWardScope()) {
            return true;
        }
        if (!currentUser.hasTdpScope()) {
            return false;
        }
        if (member.getMemberRole().isWardOfficer()) {
            return false;
        }
        return OrgScopePolicy.isOwnTdp(currentUser, member.getOrganization().getId());
    }

    private ApiException outOfScope() {
        return OrgScopePolicy.outOfScope("Member is outside current user's organization scope");
    }
}

