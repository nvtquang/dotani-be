package com.dotani.member.mapper;

import com.dotani.auth.entity.UserAccount;
import com.dotani.auth.repository.UserAccountRepository;
import com.dotani.member.dto.MemberDirectoryResponse;
import com.dotani.member.dto.MemberResponse;
import com.dotani.member.entity.Member;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {

    private final UserAccountRepository userAccountRepository;

    public MemberMapper(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public MemberResponse toResponse(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getUserId(),
                member.getFullName(),
                member.getDateOfBirth(),
                member.getGender(),
                member.getPhone(),
                emailOf(member),
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

    // Linked members use their login email; members without an account keep a contact email.
    private String emailOf(Member member) {
        if (member.getUserId() == null) {
            return member.getEmail();
        }
        return userAccountRepository.findById(member.getUserId()).map(UserAccount::getEmail).orElse(null);
    }
}
