package com.dotani.auth.service;

import com.dotani.auth.entity.UserAccount;
import com.dotani.auth.repository.UserAccountRepository;
import com.dotani.shared.exception.ApiException;
import com.dotani.shared.security.Role;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Keeps the login account's role in sync when the member module changes a member's role. */
@Service
public class UserRoleService {

    private final UserAccountRepository userAccountRepository;

    public UserRoleService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public void updateRole(String userId, Role role) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User account not found"));
        user.setRole(role);
    }
}
