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

    // Native queries see soft-deleted members too (ids and user ids stay taken after deletion).
    @Query(value = "select count(*) from members where id = :id", nativeQuery = true)
    long countAnyById(@Param("id") String id);

    @Query(value = "select count(*) from members where user_id = :userId", nativeQuery = true)
    long countAnyByUserId(@Param("userId") String userId);

    @Query(value = "select count(*) from members where user_id = :userId and id <> :id", nativeQuery = true)
    long countAnyByUserIdAndIdNot(@Param("userId") String userId, @Param("id") String id);

    default boolean existsAnyById(String id) {
        return countAnyById(id) > 0;
    }

    default boolean existsAnyByUserId(String userId) {
        return countAnyByUserId(userId) > 0;
    }

    default boolean existsAnyByUserIdAndIdNot(String userId, String id) {
        return countAnyByUserIdAndIdNot(userId, id) > 0;
    }

    /** id and name of members, including soft-deleted ones, e.g. to show who sent an old message. */
    @Query(value = "select id, full_name from members where id in (:ids)", nativeQuery = true)
    List<Object[]> findNamesIncludingDeleted(@Param("ids") Collection<String> ids);

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

    /** Officers with one of the roles: ward-wide (organizationId null) or in one organization. */
    @Query("select member.id from Member member "
            + "where member.memberRole in :roles "
            + "and (:organizationId is null or member.organization.id = :organizationId)")
    List<String> findIdsByRoles(
            @Param("roles") Collection<MemberRole> roles,
            @Param("organizationId") String organizationId
    );

    /** id, organization id and parent organization id of every member with the status (statistics). */
    @Query("select member.id, member.organization.id, parent.id from Member member "
            + "left join member.organization.parent parent "
            + "where member.memberStatus = :status")
    List<Object[]> findPlacementsByStatus(@Param("status") MemberStatus status);

    /** id, name and avatar of members, including soft-deleted ones (authors of old comments). */
    @Query(value = "select id, full_name, avatar_url from members where id in (:ids)", nativeQuery = true)
    List<Object[]> findCardsIncludingDeleted(@Param("ids") Collection<String> ids);

    @Query("select member.memberStatus, count(member) from Member member "
            + "where (:organizationId is null or member.organization.id = :organizationId) "
            + "group by member.memberStatus")
    List<Object[]> countByStatusScoped(@Param("organizationId") String organizationId);
}
