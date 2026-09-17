package com.hcmcyu.auth.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hcmcyu.auth.dto.GoogleUserInfo;
import com.hcmcyu.auth.exception.AuthException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GoogleTokenInfoVerifier implements GoogleTokenVerifier {

    private final RestClient restClient;
    private final String googleClientId;

    public GoogleTokenInfoVerifier(
            RestClient.Builder restClientBuilder,
            @Value("${auth.google.client-id:}") String googleClientId
    ) {
        this.restClient = restClientBuilder.baseUrl("https://oauth2.googleapis.com").build();
        this.googleClientId = googleClientId;
    }

    @Override
    public GoogleUserInfo verify(String idToken) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new AuthException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GOOGLE_LOGIN_NOT_CONFIGURED",
                    "Google login is not configured"
            );
        }

        GoogleTokenInfo tokenInfo;
        try {
            tokenInfo = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/tokeninfo").queryParam("id_token", idToken).build())
                    .retrieve()
                    .body(GoogleTokenInfo.class);
        } catch (RestClientException exception) {
            throw invalidGoogleToken();
        }

        if (tokenInfo == null
                || tokenInfo.subject() == null
                || tokenInfo.email() == null
                || !googleClientId.equals(tokenInfo.audience())
                || !"true".equalsIgnoreCase(tokenInfo.emailVerified())) {
            throw invalidGoogleToken();
        }

        return new GoogleUserInfo(
                tokenInfo.subject(),
                tokenInfo.email().trim().toLowerCase(),
                tokenInfo.name()
        );
    }

    private AuthException invalidGoogleToken() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Google token is invalid");
    }

    private record GoogleTokenInfo(
            @JsonProperty("sub")
            String subject,

            @JsonProperty("aud")
            String audience,

            String email,

            @JsonProperty("email_verified")
            String emailVerified,

            String name
    ) {
    }
}
