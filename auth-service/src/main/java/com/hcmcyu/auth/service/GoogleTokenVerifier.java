package com.hcmcyu.auth.service;

import com.hcmcyu.auth.dto.GoogleUserInfo;

public interface GoogleTokenVerifier {

    GoogleUserInfo verify(String idToken);
}
