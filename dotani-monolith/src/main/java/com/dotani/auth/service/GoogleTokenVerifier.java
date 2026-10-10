package com.dotani.auth.service;

import com.dotani.auth.dto.GoogleUserInfo;

public interface GoogleTokenVerifier {

    GoogleUserInfo verify(String idToken);
}
