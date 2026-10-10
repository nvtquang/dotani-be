package com.dotani.member.repository;

import com.dotani.member.entity.Member;
import com.dotani.member.entity.MemberRole;
import com.dotani.member.entity.MemberStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, String>, JpaSpecificationExecutor<Member> {

    boolean existsByUserId(String userId);

    Optional<Member> findByUserId(String userId);

    boolean existsByUserIdAndIdNot(String userId, String id);

    long countByMemberRole(MemberRole memberRole);

    long countByOrganization_Id(String organizationId);

    long countByOrganization_IdAndMemberRoleIn(String organizationId, Collection<MemberRole> memberRoles);

    long countByMemberRoleIn(Collection<MemberRole> memberRoles);

    @Query("select member.id from Member member "
            + "left join member.organization.parent parent "
            + "where (member.organization.id = :organizationId or parent.id = :organizationId) "
            + "and member.memberStatus = :status")
    List<String> findIdsInOrganizationTreeAndStatus(
            @Param("organizationId") String organizationId,
            @Param("status") MemberStatus status
    );

    @Query("select member.memberStatus, count(member) from Member member "
            + "where (:organizationId is null or member.organization.id = :organizationId) "
            + "group by member.memberStatus")
    List<Object[]> countByStatusScoped(@Param("organizationId") String organizationId);
}
