package com.hcmcyu.member.mapper;

import com.hcmcyu.member.dto.MemberDirectoryResponse;
import com.hcmcyu.member.dto.MemberResponse;
import com.hcmcyu.member.entity.Member;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {

    public MemberResponse toResponse(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getUserId(),
                member.getFullName(),
                member.getDateOfBirth(),
                member.getGender(),
                member.getPhone(),
                member.getEmail(),
                member.getAddress(),
                member.getAvatarUrl(),
                member.getYouthUnionJoinDate(),
                member.getMemberStatus(),
                member.getMemberRole(),
                member.getOrganization().getId(),
                member.getOrganization().getName(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }

    public MemberDirectoryResponse toDirectoryResponse(Member member) {
        return new MemberDirectoryResponse(
                member.getId(),
                member.getFullName(),
                member.getOrganization().getId(),
                member.getOrganization().getName(),
                member.getAvatarUrl()
        );
    }
}
