package com.hcmcyu.member.service;

import com.hcmcyu.member.entity.Member;
import com.hcmcyu.member.exception.MemberServiceException;
import com.hcmcyu.member.security.CurrentUser;
import org.springframework.http.HttpStatus;
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
        return currentUser.hasTdpScope()
                && currentUser.tdpId() != null
                && currentUser.tdpId().equals(organizationId);
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
            return currentUser.tdpId() != null
                    && currentUser.tdpId().equals(member.getOrganization().getId());
        }
        return currentUser.isMember()
                && currentUser.memberId() != null
                && currentUser.memberId().equals(member.getId());
    }

    private boolean canWrite(CurrentUser currentUser, Member member) {
        if (currentUser.hasWardScope()) {
            return true;
        }
        if (!currentUser.hasTdpScope()) {
            return false;
        }
        if (member.getMemberRole().isWardOfficer()) {
            return false;
        }
        return currentUser.tdpId() != null
                && currentUser.tdpId().equals(member.getOrganization().getId());
    }

    private MemberServiceException outOfScope() {
        return new MemberServiceException(
                HttpStatus.FORBIDDEN,
                "OUT_OF_SCOPE",
                "Member is outside current user's organization scope"
        );
    }
}

