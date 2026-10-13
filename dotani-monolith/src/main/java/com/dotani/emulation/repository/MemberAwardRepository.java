package com.dotani.emulation.repository;

import com.dotani.emulation.entity.MemberAward;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberAwardRepository extends JpaRepository<MemberAward, String> {

    List<MemberAward> findByMemberIdOrderByDecisionDateDescCreatedAtDesc(String memberId);

    List<MemberAward> findByMemberIdInAndDecisionDateBetween(Collection<String> memberIds, LocalDate from, LocalDate to);
}
