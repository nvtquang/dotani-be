package com.hcmcyu.auth.service;

import com.hcmcyu.auth.dto.GoogleLoginRequest;
import com.hcmcyu.auth.dto.MemberRegistrationResponse;
import com.hcmcyu.auth.dto.RegisterRequest;
import com.hcmcyu.auth.entity.UserAccount;

public interface MemberRegistrationClient {

    MemberRegistrationResponse createMemberProfile(UserAccount user, RegisterRequest request);

    MemberRegistrationResponse createMemberProfile(UserAccount user, GoogleLoginRequest request);

    MemberRegistrationResponse completeMemberProfile(UserAccount user, GoogleLoginRequest request);
}
