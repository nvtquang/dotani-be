package com.hcmcyu.auth.service;

import com.hcmcyu.auth.dto.InternalRoleUpdateRequest;
import com.hcmcyu.auth.dto.UserResponse;
import com.hcmcyu.auth.entity.UserAccount;
import com.hcmcyu.auth.exception.AuthException;
import com.hcmcyu.auth.mapper.UserMapper;
import com.hcmcyu.auth.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalUserService {

    private final UserAccountRepository userAccountRepository;
    private final UserMapper userMapper;
    private final String internalSecret;

    public InternalUserService(
            UserAccountRepository userAccountRepository,
            UserMapper userMapper,
            @Value("${auth.internal.secret:dev-internal-secret}") String internalSecret
    ) {
        this.userAccountRepository = userAccountRepository;
        this.userMapper = userMapper;
        this.internalSecret = internalSecret;
    }

    @Transactional
    public UserResponse updateRole(String userId, InternalRoleUpdateRequest request, String providedSecret) {
        if (providedSecret == null || !providedSecret.equals(internalSecret)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_INTERNAL_SECRET", "Invalid internal secret");
        }

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User account not found"));
        user.setRole(request.role());
        return userMapper.toResponse(user);
    }
}
