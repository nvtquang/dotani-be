package com.hcmcyu.member.service;

import com.hcmcyu.member.entity.Member;
import com.hcmcyu.member.security.CurrentUser;

public interface AuditClient {

    void record(AuditAction action, Member member, CurrentUser currentUser);
}
