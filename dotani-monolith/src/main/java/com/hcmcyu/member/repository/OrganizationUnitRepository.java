package com.hcmcyu.member.repository;

import com.hcmcyu.member.entity.OrganizationUnit;
import com.hcmcyu.member.entity.OrganizationUnitType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationUnitRepository extends JpaRepository<OrganizationUnit, String> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, String id);

    List<OrganizationUnit> findByType(OrganizationUnitType type);

    List<OrganizationUnit> findByParentId(String parentId);

    Optional<OrganizationUnit> findByIdAndType(String id, OrganizationUnitType type);
}

