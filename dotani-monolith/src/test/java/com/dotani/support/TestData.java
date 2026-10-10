package com.dotani.support;

import com.dotani.auth.entity.UserAccount;
import com.dotani.auth.repository.UserAccountRepository;
import com.dotani.member.entity.Member;
import com.dotani.member.entity.MemberRole;
import com.dotani.member.entity.MemberStatus;
import com.dotani.member.entity.OrganizationUnit;
import com.dotani.member.entity.OrganizationUnitType;
import com.dotani.member.repository.MemberRepository;
import com.dotani.member.repository.OrganizationUnitRepository;
import com.dotani.shared.security.Role;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Builds organizations and members directly through repositories. */
@Component
public class TestData {

    private final OrganizationUnitRepository organizationUnitRepository;
    private final MemberRepository memberRepository;
    private final UserAccountRepository userAccountRepository;

    public TestData(
            OrganizationUnitRepository organizationUnitRepository,
            MemberRepository memberRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.organizationUnitRepository = organizationUnitRepository;
        this.memberRepository = memberRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public OrganizationUnit ward() {
        return organization("Phuong Thuong Cat", "THUONG_CAT", OrganizationUnitType.WARD, null);
    }

    public OrganizationUnit tdp(OrganizationUnit ward, int number) {
        return organization("Chi doan TDP " + number, "TDP_" + number, OrganizationUnitType.YOUTH_UNION_BRANCH, ward);
    }

    public OrganizationUnit organization(String name, String code, OrganizationUnitType type, OrganizationUnit parent) {
        return organization(name, code, type, parent, null);
    }

    /** With a fixed id, for tests whose requests reference organizations by literal id. */
    public OrganizationUnit organization(
            String name,
            String code,
            OrganizationUnitType type,
            OrganizationUnit parent,
            String id
    ) {
        OrganizationUnit organizationUnit = new OrganizationUnit();
        organizationUnit.setId(id);
        organizationUnit.setName(name);
        organizationUnit.setCode(code);
        organizationUnit.setType(type);
        organizationUnit.setParent(parent);
        return organizationUnitRepository.save(organizationUnit);
    }

    public Member member(String fullName, OrganizationUnit organization, MemberRole role) {
        String key = UUID.randomUUID().toString().substring(0, 8);
        Member member = new Member();
        member.setFullName(fullName);
        member.setUserId("user-" + key);
        member.setEmail(key + "@dotani.test");
        member.setMemberStatus(MemberStatus.ACTIVE);
        member.setMemberRole(role);
        member.setOrganization(organization);
        return memberRepository.save(member);
    }

    public Member save(Member member) {
        return memberRepository.save(member);
    }

    /** The login account behind a member, needed by flows that sync the account (e.g. role changes). */
    public UserAccount account(Member member) {
        UserAccount user = new UserAccount();
        user.setId(member.getUserId());
        user.setUsername(member.getUserId());
        user.setEmail(member.getUserId() + "@dotani.test");
        user.setPasswordHash("{noop}unused");
        user.setRole(Role.valueOf(member.getMemberRole().name()));
        user.setMemberId(member.getId());
        user.setEnabled(true);
        return userAccountRepository.save(user);
    }

    public Role accountRole(String userId) {
        return userAccountRepository.findById(userId).orElseThrow().getRole();
    }

    /** A ward with two TDPs and one member per role, the fixture most tests start from. */
    public Fixture standardFixture() {
        OrganizationUnit ward = ward();
        OrganizationUnit tdp1 = tdp(ward, 1);
        OrganizationUnit tdp2 = tdp(ward, 2);
        return new Fixture(
                ward,
                tdp1,
                tdp2,
                member("Bi thu phuong", ward, MemberRole.WARD_SECRETARY),
                member("Pho bi thu phuong", ward, MemberRole.WARD_DEPUTY_SECRETARY),
                member("Bi thu TDP 1", tdp1, MemberRole.TDP_SECRETARY),
                member("Pho bi thu TDP 1", tdp1, MemberRole.TDP_DEPUTY_SECRETARY),
                member("Doan vien TDP 1", tdp1, MemberRole.MEMBER),
                member("Bi thu TDP 2", tdp2, MemberRole.TDP_SECRETARY),
                member("Doan vien TDP 2", tdp2, MemberRole.MEMBER)
        );
    }

    public record Fixture(
            OrganizationUnit ward,
            OrganizationUnit tdp1,
            OrganizationUnit tdp2,
            Member wardSecretary,
            Member wardDeputy,
            Member tdp1Secretary,
            Member tdp1Deputy,
            Member tdp1Member,
            Member tdp2Secretary,
            Member tdp2Member
    ) {
    }
}
