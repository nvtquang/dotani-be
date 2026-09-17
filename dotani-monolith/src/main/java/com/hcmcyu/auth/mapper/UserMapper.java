package com.hcmcyu.auth.mapper;

import com.hcmcyu.auth.dto.UserResponse;
import com.hcmcyu.auth.entity.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getMemberId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getOrganizationId(),
                user.getTdpId()
        );
    }
}

