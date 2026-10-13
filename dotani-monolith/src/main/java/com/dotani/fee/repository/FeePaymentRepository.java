package com.dotani.fee.repository;

import com.dotani.fee.entity.FeePayment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeePaymentRepository extends JpaRepository<FeePayment, String> {

    List<FeePayment> findByPeriodId(String periodId);

    List<FeePayment> findByPeriodIdIn(Collection<String> periodIds);

    Optional<FeePayment> findByPeriodIdAndMemberId(String periodId, String memberId);

    Optional<FeePayment> findByIdAndPeriodId(String id, String periodId);

    boolean existsByPeriodId(String periodId);
}
