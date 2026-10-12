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
import com.dotani.member.service.MemberAccessService;
import com.dotani.shared.security.Role;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Builds organizations and members directly through repositories. */
@Component
public class TestData {

    private final OrganizationUnitRepository organizationUnitRepository;
    private final MemberRepository memberRepository;
    private final UserAccountRepository userAccountRepository;
    private final MemberAccessService memberAccessService;

    public TestData(
            OrganizationUnitRepository organizationUnitRepository,
            MemberRepository memberRepository,
            UserAccountRepository userAccountRepository,
            MemberAccessService memberAccessService
    ) {
        this.organizationUnitRepository = organizationUnitRepository;
        this.memberRepository = memberRepository;
        this.userAccountRepository = userAccountRepository;
        this.memberAccessService = memberAccessService;
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

    /** A member with its own login account, linked both ways like a registered user. */
    public Member member(String fullName, OrganizationUnit organization, MemberRole role) {
        String userId = ensureAccount("user-" + UUID.randomUUID().toString().substring(0, 8));
        Member member = new Member();
        member.setFullName(fullName);
        member.setUserId(userId);
        member.setMemberStatus(MemberStatus.ACTIVE);
        member.setMemberRole(role);
        member.setOrganization(organization);
        Member saved = memberRepository.save(member);
        account(saved);
        return saved;
    }

    public Member save(Member member) {
        return memberRepository.save(member);
    }

    /**
     * Creates a login account with this id if none exists (members.user_id must reference one).
     * Returns the id so it can be used inline: {@code member.setUserId(data.ensureAccount("user-1"))}.
     */
    public String ensureAccount(String userId) {
        if (!userAccountRepository.existsById(userId)) {
            UserAccount user = new UserAccount();
            user.setId(userId);
            user.setUsername(userId);
            user.setEmail(userId + "@dotani.test");
            user.setPasswordHash("{noop}unused");
            user.setEnabled(true);
            userAccountRepository.save(user);
        }
        return userId;
    }

    /** Links the member's login account back to the member. */
    public UserAccount account(Member member) {
        ensureAccount(member.getUserId());
        UserAccount user = userAccountRepository.findById(member.getUserId()).orElseThrow();
        user.setMemberId(member.getId());
        return userAccountRepository.save(user);
    }

    /** The role a token issued for this account would carry (derived from its member profile). */
    public Role accountRole(String userId) {
        return memberAccessService.scopeOf(userAccountRepository.findById(userId).orElseThrow().getMemberId()).role();
    }

    /**
     * Organizations and members under the literal ids older tests reference ("ward-thuong-cat", "tdp-1",
     * "member-1", ...). Foreign keys require those rows to exist.
     */
    public void legacyIds() {
        OrganizationUnit ward = organization("Phuong Thuong Cat", "THUONG_CAT", OrganizationUnitType.WARD, null, "ward-thuong-cat");
        OrganizationUnit tdp1 = organization("Chi doan TDP 1", "TDP_1", OrganizationUnitType.YOUTH_UNION_BRANCH, ward, "tdp-1");
        OrganizationUnit tdp2 = organization("Chi doan TDP 2", "TDP_2", OrganizationUnitType.YOUTH_UNION_BRANCH, ward, "tdp-2");
        for (int i = 1; i <= 4; i++) {
            memberWithId("member-" + i, "Member " + i, tdp1, MemberRole.MEMBER);
        }
        memberWithId("ward-secretary-member", "Ward Secretary", ward, MemberRole.WARD_SECRETARY);
        memberWithId("ward-secretary", "Ward Secretary 2", ward, MemberRole.WARD_SECRETARY);
        memberWithId("tdp-secretary-member", "TDP 1 Secretary", tdp1, MemberRole.TDP_SECRETARY);
        memberWithId("tdp1-secretary", "TDP 1 Secretary 2", tdp1, MemberRole.TDP_SECRETARY);
        memberWithId("tdp2-secretary", "TDP 2 Secretary", tdp2, MemberRole.TDP_SECRETARY);
    }

    public Member memberWithId(String id, String fullName, OrganizationUnit organization, MemberRole role) {
        Member member = new Member();
        member.setId(id);
        member.setFullName(fullName);
        member.setUserId(ensureAccount(id + "-account"));
        member.setMemberStatus(MemberStatus.ACTIVE);
        member.setMemberRole(role);
        member.setOrganization(organization);
        Member saved = memberRepository.save(member);
        account(saved);
        return saved;
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
