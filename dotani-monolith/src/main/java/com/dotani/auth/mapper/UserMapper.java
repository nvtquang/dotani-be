package com.dotani.auth.mapper;

import com.dotani.shared.security.AccessScope;
import com.dotani.auth.dto.UserResponse;
import com.dotani.auth.entity.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(UserAccount user, AccessScope scope) {
        return new UserResponse(
                user.getId(),
                user.getMemberId(),
                user.getUsername(),
                user.getEmail(),
                scope.role(),
                scope.organizationId(),
                scope.tdpId()
        );
    }
}

