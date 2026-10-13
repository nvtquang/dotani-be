package com.dotani.emulation.repository;

import com.dotani.emulation.entity.MemberRating;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRatingRepository extends JpaRepository<MemberRating, String> {

    List<MemberRating> findByMemberIdOrderByYearDesc(String memberId);

    Optional<MemberRating> findByMemberIdAndYear(String memberId, int year);

    List<MemberRating> findByYearAndMemberIdIn(int year, Collection<String> memberIds);
}
